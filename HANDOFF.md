# Handoff

State on 2026-10-14: version 2.4.0 builds, signed release in `release/` (gitignored), tested
end to end on an API 34 emulator against rapi.highda.space, directly and behind a local
Basic-auth proxy. Not published anywhere yet; `upstream` remote is GitLab cosmosapps/stealth,
no `origin`. The About screen links to https://github.com/highda/stealth-continued.

## Build

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew assembleRelease
```

SDK: `/opt/homebrew/share/android-commandlinetools` (in `local.properties`). Release key:
`~/.android/stealth-continued/release.jks`, passwords in `keystore.properties` (repo root,
gitignored; copy next to the key). Losing the key means users must reinstall to update.

## Pinned on purpose

- Navigation runtime 2.5.3: 2.6+ crashes because the post screen is added to the NavHost's
  FragmentManager by hand (`BaseFragment.onClick`). Fix properly by making it a nav destination.
- `android.enableR8.fullMode=false`: full mode strips annotations Moshi's custom adapters need.
- `android.nonTransitiveRClass=false`: the code uses library attributes through its own `R`.

## Workarounds for redlib-tor-api (remove when the server is fixed)

- `/r/a+b` reaches Reddit with a literal `+` (fork's `src/raw.rs`), answered with r/popular:
  `TedditSource.getMultiSubreddit` searches `subreddit:a OR subreddit:b` instead.
- No `/api/morechildren` (not on `/raw`'s allowlist): `TedditSource.getMoreChildren` rebuilds
  it from comment permalinks.
- No user search route: `TedditSource.searchUser` looks the query up as an exact username.

## Testing

Emulator AVD `stealth34`. The test proxies used (Basic auth + request log, host logger set as
the emulator's global proxy) were throwaway scripts; see the session notes in memory.
