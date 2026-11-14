#!/usr/bin/env bash
set -uo pipefail

BASE="${BASE:-http://localhost:8080}"
PASSWORD="pass-$(date +%s)-$RANDOM"
SUFFIX="$(date +%s)-$RANDOM"
ALICE_LOGIN="alice-$SUFFIX"
BOB_LOGIN="bob-$SUFFIX"
PASS=0
FAIL=0

for tool in curl jq; do
  command -v "$tool" >/dev/null || { echo "missing $tool"; exit 1; }
done

step() { printf '\n== %s\n' "$1"; }
ok() { printf '  ok   %s\n' "$1"; PASS=$((PASS + 1)); }
bad() { printf '  FAIL %s (expected %s, got %s)\n' "$1" "$2" "$3"; FAIL=$((FAIL + 1)); }

expect() {
  local label="$1" expected="$2" actual="$3"
  if [ "$actual" = "$expected" ]; then ok "$label"; else bad "$label" "$expected" "$actual"; fi
}

status() { curl -s -o /dev/null -w '%{http_code}' "$@"; }
auth() { printf 'Authorization: Bearer %s' "$1"; }
present() { [ -n "$1" ] && [ "$1" != null ]; }

# Later steps depend on earlier setup (tokens, ITEM_ID), so stop instead of
# producing a long series of unclear failures.
abort_on_failure() {
  if [ "$FAIL" -gt 0 ]; then
    printf '\nsetup failed, aborting: %d passed, %d failed\n' "$PASS" "$FAIL"
    exit 1
  fi
}

register() {
  curl -s -X POST "$BASE/register" -H 'Content-Type: application/json' \
    -d "$(jq -cn --arg login "$1" --arg password "$PASSWORD" '{login: $login, password: $password}')"
}

token_for() {
  curl -s -X POST "$BASE/login" -H 'Content-Type: application/json' \
    -d "$(jq -cn --arg login "$1" --arg password "$PASSWORD" '{login: $login, password: $password}')" \
    | jq -r .token
}

step "register and login"
ALICE_JSON=$(register "$ALICE_LOGIN")
BOB_JSON=$(register "$BOB_LOGIN")
ALICE_ID=$(jq -r .id <<<"$ALICE_JSON")
BOB_ID=$(jq -r .id <<<"$BOB_JSON")
ALICE=$(token_for "$ALICE_LOGIN")
BOB=$(token_for "$BOB_LOGIN")
expect "alice id" "true" "$(present "$ALICE_ID" && echo true || echo false)"
expect "bob id" "true" "$(present "$BOB_ID" && echo true || echo false)"
expect "alice token" "true" "$(present "$ALICE" && echo true || echo false)"
expect "bob token" "true" "$(present "$BOB" && echo true || echo false)"
abort_on_failure

step "create and list"
CREATE=$(curl -s -w '\n%{http_code}' -X POST "$BASE/items" -H "$(auth "$ALICE")" \
  -H 'Content-Type: application/json' -d '{"title":"pierwsza","content":"treść"}')
ITEM_JSON=$(sed '$d' <<<"$CREATE")
ITEM_ID=$(jq -r .id <<<"$ITEM_JSON")
expect "create status" "201" "$(tail -n1 <<<"$CREATE")"
expect "item id" "true" "$(present "$ITEM_ID" && echo true || echo false)"
abort_on_failure
expect "initial version" "0" "$(jq -r .version <<<"$ITEM_JSON")"
expect "owner" "$ALICE_ID" "$(jq -r .ownerId <<<"$ITEM_JSON")"
expect "myRole" "OWNER" "$(curl -s "$BASE/items" -H "$(auth "$ALICE")" | jq -r '.[0].myRole')"

step "owner edit and version conflict"
PATCH_JSON=$(curl -s -X PATCH "$BASE/items/$ITEM_ID" -H "$(auth "$ALICE")" \
  -H 'Content-Type: application/json' -d '{"title":"druga","version":0}')
expect "version after edit" "1" "$(jq -r .version <<<"$PATCH_JSON")"
expect "title after edit" "druga" "$(jq -r .title <<<"$PATCH_JSON")"
CONFLICT=$(curl -s -w '\n%{http_code}' -X PATCH "$BASE/items/$ITEM_ID" -H "$(auth "$ALICE")" \
  -H 'Content-Type: application/json' -d '{"title":"trzecia","version":0}')
expect "conflict status" "409" "$(tail -n1 <<<"$CONFLICT")"
expect "conflict version" "1" "$(sed '$d' <<<"$CONFLICT" | jq -r .version)"
expect "patch without version" "400" "$(status -X PATCH "$BASE/items/$ITEM_ID" -H "$(auth "$ALICE")" \
  -H 'Content-Type: application/json' -d '{"title":"x"}')"

step "stranger and missing token"
expect "stranger patch" "403" "$(status -X PATCH "$BASE/items/$ITEM_ID" -H "$(auth "$BOB")" \
  -H 'Content-Type: application/json' -d '{"title":"hack","version":1}')"
expect "stranger history" "403" "$(status "$BASE/items/$ITEM_ID/history" -H "$(auth "$BOB")")"
expect "stranger delete" "403" "$(status -X DELETE "$BASE/items/$ITEM_ID" -H "$(auth "$BOB")")"
expect "non-owner share" "403" "$(status -X POST "$BASE/items/$ITEM_ID/share" -H "$(auth "$BOB")" \
  -H 'Content-Type: application/json' -d "{\"userId\":\"$BOB_ID\",\"role\":\"EDITOR\"}")"
expect "stranger list" "0" "$(curl -s "$BASE/items" -H "$(auth "$BOB")" | jq length)"
expect "no token" "401" "$(status "$BASE/items")"
expect "bad token" "401" "$(status "$BASE/items" -H 'Authorization: Bearer garbage')"

step "share, viewer and editor"
expect "share viewer" "201" "$(status -X POST "$BASE/items/$ITEM_ID/share" -H "$(auth "$ALICE")" \
  -H 'Content-Type: application/json' -d "{\"userId\":\"$BOB_ID\",\"role\":\"VIEWER\"}")"
expect "bob role" "VIEWER" "$(curl -s "$BASE/items" -H "$(auth "$BOB")" | jq -r '.[0].myRole')"
expect "viewer patch" "403" "$(status -X PATCH "$BASE/items/$ITEM_ID" -H "$(auth "$BOB")" \
  -H 'Content-Type: application/json' -d '{"title":"hack","version":1}')"
expect "viewer history" "200" "$(status "$BASE/items/$ITEM_ID/history" -H "$(auth "$BOB")")"
expect "self share" "400" "$(status -X POST "$BASE/items/$ITEM_ID/share" -H "$(auth "$ALICE")" \
  -H 'Content-Type: application/json' -d "{\"userId\":\"$ALICE_ID\",\"role\":\"VIEWER\"}")"
expect "unknown user" "404" "$(status -X POST "$BASE/items/$ITEM_ID/share" -H "$(auth "$ALICE")" \
  -H 'Content-Type: application/json' -d '{"userId":"11111111-1111-1111-1111-111111111111","role":"VIEWER"}')"
expect "share editor" "200" "$(status -X POST "$BASE/items/$ITEM_ID/share" -H "$(auth "$ALICE")" \
  -H 'Content-Type: application/json' -d "{\"userId\":\"$BOB_ID\",\"role\":\"EDITOR\"}")"
EDITOR_JSON=$(curl -s -X PATCH "$BASE/items/$ITEM_ID" -H "$(auth "$BOB")" \
  -H 'Content-Type: application/json' -d '{"content":"edit by bob","version":1}')
expect "editor version" "2" "$(jq -r .version <<<"$EDITOR_JSON")"
expect "history revisions" "3" "$(curl -s "$BASE/items/$ITEM_ID/history" -H "$(auth "$ALICE")" | jq length)"
expect "last changedBy" "$BOB_LOGIN" "$(curl -s "$BASE/items/$ITEM_ID/history" -H "$(auth "$ALICE")" | jq -r '.[-1].changedBy')"

step "revoke"
expect "revoke" "204" "$(status -X DELETE "$BASE/items/$ITEM_ID/share/$BOB_ID" -H "$(auth "$ALICE")")"
expect "after revoke patch" "403" "$(status -X PATCH "$BASE/items/$ITEM_ID" -H "$(auth "$BOB")" \
  -H 'Content-Type: application/json' -d '{"title":"po","version":2}')"
expect "after revoke list" "0" "$(curl -s "$BASE/items" -H "$(auth "$BOB")" | jq length)"
expect "revoke again" "404" "$(status -X DELETE "$BASE/items/$ITEM_ID/share/$BOB_ID" -H "$(auth "$ALICE")")"

step "soft delete"
expect "delete" "204" "$(status -X DELETE "$BASE/items/$ITEM_ID" -H "$(auth "$ALICE")")"
expect "list after delete" "0" "$(curl -s "$BASE/items" -H "$(auth "$ALICE")" | jq length)"
expect "patch deleted" "404" "$(status -X PATCH "$BASE/items/$ITEM_ID" -H "$(auth "$ALICE")" \
  -H 'Content-Type: application/json' -d '{"title":"po","version":2}')"
expect "history deleted" "404" "$(status "$BASE/items/$ITEM_ID/history" -H "$(auth "$ALICE")")"
expect "delete again" "404" "$(status -X DELETE "$BASE/items/$ITEM_ID" -H "$(auth "$ALICE")")"

printf '\n%d passed, %d failed\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ]