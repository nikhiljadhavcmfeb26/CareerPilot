#!/usr/bin/env sh
# Initialises careerpilot-config-repo as a git repository.
#
# Required because the Spring Cloud Config Server reads COMMITTED git content,
# and .git/ is excluded from the distributed archive. Without this, config-server
# fails to start and every other service fails with it.
#
# Safe to re-run: on an already-initialised repo it just commits pending changes.
set -e
cd "$(dirname "$0")/careerpilot-config-repo"

# Fall back to a local identity if git has no global user configured, so this
# works on a fresh machine without forcing the user to set one up first.
if git config user.email >/dev/null 2>&1 || git config --global user.email >/dev/null 2>&1; then
  GIT="git"
else
  echo "No git identity configured - using a local one for this repository only."
  GIT="git -c user.name=CareerPilot -c user.email=config@careerpilot.local"
fi

if [ -d .git ]; then
  echo "Config repo is already a git repository."
  git add -A
  if git diff --cached --quiet; then
    echo "Nothing to commit - working tree is clean."
  else
    $GIT commit -m "Update CareerPilot configuration"
    echo "Committed pending configuration changes."
  fi
else
  git init -b main >/dev/null 2>&1 || { git init >/dev/null && git checkout -b main >/dev/null 2>&1; }
  git add .
  $GIT commit -m "Initial CareerPilot configuration" >/dev/null
  echo "Initialised config repo on branch 'main'."
fi

echo
git log --oneline
echo
echo "Done. You can now start config-server."
