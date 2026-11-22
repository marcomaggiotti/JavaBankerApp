#!/usr/bin/env bash
# Coding-challenge exercises: statement + starter code + tests + reference solution.
#
#   scripts/exercises.sh list               list exercises
#   scripts/exercises.sh start ex03         (re)set src/main to the starter code (your changes are lost!)
#   scripts/exercises.sh test ex03          run the exercise's tests (red until you solve it)
#   scripts/exercises.sh solve ex03         replace your code with the reference solution
#   scripts/exercises.sh diff ex03          compare your code with the reference solution
#   scripts/exercises.sh check-solutions    verify that every reference solution passes (CI), then restore starters
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
EXERCISES="$ROOT/exercises"
MAIN_PKG="$ROOT/src/main/java/com/javabanker/exercises"

usage() { sed -n '2,10p' "$0" | sed 's/^# \{0,1\}//'; exit 1; }

exercise_dir() {
  local id="$1" dir
  [[ "$id" =~ ^ex[0-9]{2}$ ]] || { echo "Exercise id must look like ex03" >&2; exit 1; }
  dir="$(find "$EXERCISES" -maxdepth 1 -type d -name "${id}-*" | head -1)"
  [[ -n "$dir" ]] || { echo "Unknown exercise $id (see: $0 list)" >&2; exit 1; }
  echo "$dir"
}

install_variant() { # $1 = id, $2 = starter|solution
  local dir target
  dir="$(exercise_dir "$1")"
  target="$MAIN_PKG/$1"
  rm -rf "$target"
  mkdir -p "$target"
  cp "$dir/$2/"*.java "$target/"
}

run_tests() { # $1 = test filter
  (cd "$ROOT" && mvn -B -q test -Dexercise.excludedGroups=none -Dgroups=exercise \
      -Dtest="$1" -Dsurefire.failIfNoSpecifiedTests=false)
}

cmd="${1:-}"; id="${2:-}"
case "$cmd" in
  list)
    for d in "$EXERCISES"/ex*/; do
      printf '%-30s %s\n' "$(basename "$d")" "$(head -1 "$d/README.md" | sed 's/^# //')"
    done
    ;;
  start)
    [[ -n "$id" ]] || usage
    install_variant "$id" starter
    echo "Starter code for $id installed in src/main/java/com/javabanker/exercises/$id"
    echo "Read $(exercise_dir "$id")/README.md, then: $0 test $id"
    ;;
  test)
    [[ -n "$id" ]] || usage
    exercise_dir "$id" > /dev/null
    if run_tests "com.javabanker.exercises.$id.**"; then
      echo "✅ $id: all tests pass"
    else
      echo "❌ $id: some tests fail (see output above)"; exit 1
    fi
    ;;
  solve)
    [[ -n "$id" ]] || usage
    install_variant "$id" solution
    echo "Reference solution for $id installed. Use '$0 start $id' to try again from scratch."
    ;;
  diff)
    [[ -n "$id" ]] || usage
    diff -ru "$(exercise_dir "$id")/solution" "$MAIN_PKG/$id" || true
    ;;
  check-solutions)
    restore() { for d in "$EXERCISES"/ex*/; do install_variant "$(basename "$d" | cut -c1-4)" starter; done; }
    trap restore EXIT
    for d in "$EXERCISES"/ex*/; do install_variant "$(basename "$d" | cut -c1-4)" solution; done
    run_tests "com.javabanker.exercises.**"
    echo "✅ all reference solutions pass"
    ;;
  *)
    usage
    ;;
esac
