#!/usr/bin/env bash
# Coding-challenge exercises: statement + starter code + tests + reference solution.
#
# Exercises ship SOLVED: src/main contains the reference solutions.
#
#   scripts/exercises.sh list               list exercises
#   scripts/exercises.sh test ex03          run the exercise's tests
#   scripts/exercises.sh start ex03         practise: replace the solution with the starter code (red tests)
#   scripts/exercises.sh solve ex03         put the reference solution back (your changes are lost!)
#   scripts/exercises.sh diff ex03          compare your code with the reference solution
#   scripts/exercises.sh check-solutions    verify that every reference solution passes (CI)
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
EXERCISES="$ROOT/exercises"
MAIN_PKG="$ROOT/src/main/java/com/javabanker/exercises"

usage() { sed -n '2,11p' "$0" | sed 's/^# \{0,1\}//'; exit 1; }

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
    echo "While practising, skip exercise tests in the full build: mvn test -Dexercise.excludedGroups=exercise"
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
    echo "Reference solution for $id installed (the default state)."
    ;;
  diff)
    [[ -n "$id" ]] || usage
    diff -ru "$(exercise_dir "$id")/solution" "$MAIN_PKG/$id" || true
    ;;
  check-solutions)
    for d in "$EXERCISES"/ex*/; do install_variant "$(basename "$d" | cut -c1-4)" solution; done
    run_tests "com.javabanker.exercises.**"
    echo "✅ all reference solutions pass"
    ;;
  *)
    usage
    ;;
esac
