#!/usr/bin/env bash
# Determines the next semantic version based on Conventional Commits
# messages since the last release tag, and exposes the result via
# GITHUB_OUTPUT for use in the release workflow.
#
# Bump rules:
#   - Commit contains "BREAKING CHANGE" in the body, or the type/scope is
#     followed by "!" (e.g. "feat!: ..." / "fix(api)!: ...")  -> MAJOR bump
#   - Commit type is "feat"                                    -> MINOR bump
#   - Anything else (fix, chore, docs, refactor, ...)          -> PATCH bump
#
# The highest bump level found among all commits since the last tag wins.
set -euo pipefail

last_tag="$(git describe --tags --abbrev=0 --match 'v[0-9]*.[0-9]*.[0-9]*' 2>/dev/null || true)"

if [[ -z "${last_tag}" ]]; then
  echo "No previous release tag found. Preparing initial release v1.0.0."
  range="HEAD"
  base_major=1
  base_minor=0
  base_patch=0
  is_initial=true
else
  echo "Last release tag: ${last_tag}"
  range="${last_tag}..HEAD"
  version="${last_tag#v}"
  base_major="$(echo "${version}" | cut -d. -f1)"
  base_minor="$(echo "${version}" | cut -d. -f2)"
  base_patch="$(echo "${version}" | cut -d. -f3)"
  is_initial=false
fi

commit_count="$(git log ${range} --oneline | wc -l | tr -d ' ')"

if [[ "${is_initial}" == "false" && "${commit_count}" -eq 0 ]]; then
  echo "No new commits since ${last_tag}. Skipping release."
  {
    echo "should_release=false"
  } >> "${GITHUB_OUTPUT}"
  exit 0
fi

bump="patch"

if [[ "${is_initial}" == "true" ]]; then
  bump="none"
else
  while IFS= read -r -d '' record; do
    subject="${record%%$'\x01'*}"
    body="${record#*$'\x01'}"
    [[ -z "${subject}" ]] && continue

    if [[ "${body}" == *"BREAKING CHANGE"* || "${subject}" =~ ^[a-zA-Z]+(\([^\)]*\))?!: ]]; then
      bump="major"
      break
    elif [[ "${subject}" =~ ^feat(\([^\)]*\))?: ]]; then
      bump="minor"
    fi
  done < <(git log ${range} -z --pretty=format:"%s%x01%b")
fi

major="${base_major}"
minor="${base_minor}"
patch="${base_patch}"

case "${bump}" in
  none)
    # Initial release, keep base version as-is.
    ;;
  major)
    major=$((major + 1))
    minor=0
    patch=0
    ;;
  minor)
    minor=$((minor + 1))
    patch=0
    ;;
  patch)
    patch=$((patch + 1))
    ;;
esac

new_version="v${major}.${minor}.${patch}"

echo "Bump type: ${bump}"
echo "New version: ${new_version}"

{
  echo "should_release=true"
  echo "bump=${bump}"
  echo "new_version=${new_version}"
  echo "version_number=${major}.${minor}.${patch}"
  echo "previous_tag=${last_tag}"
} >> "${GITHUB_OUTPUT}"
