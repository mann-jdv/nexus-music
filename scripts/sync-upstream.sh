#!/usr/bin/env bash
# Merge SimpMusic upstream into Nexus Music. Read SYNC.md first; it explains every stop below.
#
#   1. fetch upstream (https://github.com/maxrave-dev/simpmusic, branch dev) and core's origin
#   2. core/: merge the core commit upstream pins INTO core's own Nexus branch. Upstream's pin is
#      never checked out bare: that would drop the Nexus core commits (updater removal, local
#      playback, bit-perfect) on the floor.
#   3. app repo: merge upstream/dev, then point core at the merged core commit from step 2
#      (which also settles the gitlink conflict every sync has)
#   4. auto-resolve files Nexus deleted on purpose (upstream workflows, updater code, launcher webps)
#   5. guards: refuse to commit if updater code, upstream CI workflows, or upstream branding came back
#   6. commit the merge
#
# It STOPS (non-zero exit, merge left in progress for you to finish) on any conflict or guard hit.
# After fixing, run it again with --continue.
#
# Usage: scripts/sync-upstream.sh [--branch nexus/main] [--upstream-ref dev] [--continue]
#   --branch        branch to merge into, in BOTH repos (default nexus/main)
#   --upstream-ref  upstream branch (default dev)
#   --continue      finish a merge this script stopped on (after you resolved and `git add`-ed)
#
# Exit codes: 0 merged (or nothing to do) · 1 usage/precondition · 3 core conflict
#             4 app-repo conflict · 5 guard hit
set -euo pipefail

UPSTREAM_URL="https://github.com/maxrave-dev/simpmusic.git"
BRANCH="nexus/main"
UPSTREAM_REF="dev"
CONTINUE=0
while [ $# -gt 0 ]; do
  case "$1" in
    --branch) BRANCH="$2"; shift ;;
    --upstream-ref) UPSTREAM_REF="$2"; shift ;;
    --continue) CONTINUE=1 ;;
    -h|--help) sed -n '2,24p' "$0"; exit 0 ;;
    *) echo "Unknown option: $1" >&2; exit 1 ;;
  esac
  shift
done

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
say() { printf '\n== %s\n' "$*"; }
die() { echo "ERROR: $*" >&2; exit "${2:-1}"; }

# Paths Nexus deleted on purpose. A modify/delete conflict on one of these is resolved by keeping
# it deleted. Patterns are bash globs matched against the repo-relative path.
NEXUS_DELETED=(
  ".github/workflows/android-release.yml"
  ".github/workflows/android.yml"
  ".github/workflows/desktop-package.yml"
  ".github/workflows/pr-triage.yml"
  "androidApp/src/main/res/mipmap-*/ic_launcher*.webp"
  "androidApp/src/main/res/drawable-v24/ic_launcher_foreground.xml"
)
CORE_NEXUS_DELETED=(
  "data/src/commonMain/kotlin/com/maxrave/data/repository/UpdateRepositoryImpl.kt"
  "domain/src/commonMain/kotlin/com/maxrave/domain/data/model/update/UpdateData.kt"
  "domain/src/commonMain/kotlin/com/maxrave/domain/repository/UpdateRepository.kt"
  "service/kotlinYtmusicScraper/src/commonMain/kotlin/com/maxrave/kotlinytmusicscraper/models/simpmusic/FdroidResponse.kt"
  "service/kotlinYtmusicScraper/src/commonMain/kotlin/com/maxrave/kotlinytmusicscraper/models/simpmusic/GithubResponse.kt"
)
# Code (not comments) that only the removed in-app updater used. Any hit means upstream re-added it.
UPDATER_PATTERN='checkForUpdate|shouldCheckForUpdate|isCheckingUpdate|updateResponse|UpdateRepository|UpdateData\b|GithubResponse|FdroidResponse|checkOfficialBuild|autoCheckForUpdates|updateChannel|GITHUB_FOSS_NIGHTLY|releases/latest|fdroiddata'

matches_any() {
  local path="$1"; shift
  local pat
  for pat in "$@"; do
    # shellcheck disable=SC2053
    [[ "$path" == $pat ]] && return 0
  done
  return 1
}

# Resolve "deleted by us" conflicts on the listed paths by keeping them deleted.
keep_nexus_deletions() {
  local repo="$1"; shift
  local line status path
  while IFS= read -r line; do
    [ -n "$line" ] || continue
    status="${line:0:2}"; path="${line:3}"
    if [ "$status" = "DU" ] && matches_any "$path" "$@"; then
      git -C "$repo" rm -q --cached -- "$path" 2>/dev/null || true
      rm -f -- "$repo/$path"
      echo "   kept deleted (Nexus removed it): $path"
    fi
  done < <(git -C "$repo" status --porcelain --untracked-files=no)
}

unmerged() { git -C "$1" diff --name-only --diff-filter=U; }

guards() {
  local hits=0 out
  say "Guards"
  # 1. In-app updater re-added (code only: comment lines are fine, CLAUDE.md/SYNC.md are not code).
  out=$( { git grep -nE "$UPDATER_PATTERN" -- '*.kt' '*.kts' '*.java' '*.xml' ':!**/composeResources/values-*/**' || true
           git -C core grep -nE "$UPDATER_PATTERN" -- '*.kt' '*.kts' '*.java' | sed 's#^#core/#' || true; } |
         grep -vE '^[^:]+:[0-9]+:[[:space:]]*(//|\*|/\*)' || true)
  if [ -n "$out" ]; then
    echo "!! Updater code came back (see SYNC.md, 'Re-added updater code'):"; echo "$out" | head -n 40; hits=1
  fi
  # 2. Upstream CI workflows (they build and publish with upstream's secrets and channels).
  out=$(git ls-files .github/workflows | grep -v '^.github/workflows/nexus-' || true)
  if [ -n "$out" ]; then
    echo "!! Upstream workflow(s) present; delete them (SYNC.md, 'CI'):"; echo "$out"; hits=1
  fi
  # 3. Identity drift.
  grep -q 'applicationId = "org.nexusmusic.app"' androidApp/build.gradle.kts ||
    { echo "!! androidApp applicationId is no longer org.nexusmusic.app"; hits=1; }
  grep -q 'rootProject.name = "NexusMusic"' settings.gradle.kts ||
    { echo "!! settings.gradle.kts rootProject.name is no longer NexusMusic"; hits=1; }
  grep -q 'rootProject.extra\["nexusVersionName"\]' androidApp/build.gradle.kts ||
    { echo "!! androidApp versionName no longer comes from nexus.properties"; hits=1; }
  [ $hits -eq 0 ] && echo "   ok: no updater code, no upstream workflows, identity intact"
  return $hits
}

finish() {
  # core's gitlink must point at core's merged HEAD (this also resolves the gitlink conflict).
  git add core
  keep_nexus_deletions . "${NEXUS_DELETED[@]}"
  local left; left=$(unmerged .)
  if [ -n "$left" ]; then
    say "Conflicts to resolve in the app repo (merge left in progress):"
    echo "$left" | sed 's/^/   /'
    echo; echo "Resolve them using SYNC.md, 'git add' each file, then: scripts/sync-upstream.sh --continue"
    echo "To give up instead: git merge --abort   (and in core/: git reset --hard ORIG_HEAD if it merged)"
    exit 4
  fi
  guards || { echo; echo "Fix the above (git rm / edit), 'git add', then: scripts/sync-upstream.sh --continue"; exit 5; }
  local up; up=$(git rev-parse --short "upstream/$UPSTREAM_REF")
  git commit -q --no-edit -m "Merge upstream SimpMusic $UPSTREAM_REF ($up) into $BRANCH" \
    -m "core: $(git -C core log -1 --format='%h %s')"
  say "Merged. $(git log -1 --oneline)"
  echo "Next: build and smoke-test (./gradlew -PisFullBuild=false :androidApp:assembleDebug :composeApp:compileKotlinJvm)."
  echo "Nothing was pushed."
}

# ------------------------------------------------------------------------------------------------
[ -d core/.git ] || [ -f core/.git ] || die "core/ submodule is not checked out"

if [ $CONTINUE -eq 1 ]; then
  [ -f "$(git rev-parse --git-dir)/MERGE_HEAD" ] || die "no merge in progress to continue"
  [ -z "$(unmerged core)" ] || die "core/ still has unresolved files; resolve and commit them there first" 3
  [ ! -f "$(git -C core rev-parse --git-dir)/MERGE_HEAD" ] || die "core/ merge not committed yet: cd core && git commit" 3
  finish
  exit 0
fi

for repo in . core; do
  # --ignore-submodules: core's gitlink moves when a core merge was finished by hand (resume case);
  # finish() re-points it explicitly, and core's own tree is checked on its own pass.
  [ -z "$(git -C "$repo" status --porcelain --untracked-files=no --ignore-submodules=all)" ] ||
    die "uncommitted changes in $repo; commit or stash them first"
  [ ! -f "$(git -C "$repo" rev-parse --git-dir)/MERGE_HEAD" ] ||
    die "a merge is already in progress in $repo (finish it, or use --continue)"
done
for repo in . core; do
  git -C "$repo" rev-parse --verify -q "refs/heads/$BRANCH" >/dev/null || die "branch $BRANCH does not exist in $repo"
  git -C "$repo" -c submodule.recurse=false checkout -q "$BRANCH"
done

git remote get-url upstream >/dev/null 2>&1 || { say "Adding remote upstream = $UPSTREAM_URL"; git remote add upstream "$UPSTREAM_URL"; }
say "Fetching upstream/$UPSTREAM_REF and core's origin"
git fetch -q upstream "$UPSTREAM_REF"
git -C core fetch -q origin

UP=$(git rev-parse "upstream/$UPSTREAM_REF")
if git merge-base --is-ancestor "$UP" HEAD; then
  say "Already up to date with upstream/$UPSTREAM_REF ($(git rev-parse --short "$UP"))"; exit 0
fi
echo "   upstream/$UPSTREAM_REF: $(git log -1 --format='%h %s' "$UP")"
echo "   new upstream commits:   $(git rev-list --count HEAD.."$UP")"

UPCORE=$(git ls-tree "$UP" core | awk '{print $3}')
[ -n "$UPCORE" ] || die "upstream/$UPSTREAM_REF has no core submodule entry"
say "core/: merging upstream's pinned core $UPCORE into $BRANCH"
git -C core cat-file -e "$UPCORE^{commit}" 2>/dev/null || git -C core fetch -q origin "$UPCORE"
if git -C core merge-base --is-ancestor "$UPCORE" HEAD; then
  echo "   already contains it"
elif ! git -C core merge --no-ff -q -m "Merge upstream core $(git -C core rev-parse --short "$UPCORE") into $BRANCH" "$UPCORE"; then
  keep_nexus_deletions core "${CORE_NEXUS_DELETED[@]}"
  left=$(unmerged core)
  if [ -n "$left" ]; then
    say "Conflicts in core/ (core merge left in progress):"
    echo "$left" | sed 's/^/   core\//'
    echo; echo "Resolve them (SYNC.md), then: cd core && git add <files> && git commit --no-edit"
    echo "Then run scripts/sync-upstream.sh again (it sees core already merged and goes on)."
    echo "To give up instead: cd core && git merge --abort"
    exit 3
  fi
  git -C core commit -q --no-edit
fi
echo "   core now at $(git -C core log -1 --format='%h %s')"

say "App repo: merging upstream/$UPSTREAM_REF into $BRANCH"
# --no-commit always: the gitlink and the guards have to be settled before anything is committed.
# submodule.recurse=false: core is moved by hand above, never to the bare upstream pin.
git -c submodule.recurse=false merge --no-ff --no-commit "$UP" >/dev/null 2>&1 || true
[ -f "$(git rev-parse --git-dir)/MERGE_HEAD" ] || die "git merge did not start (see 'git status')"
finish
