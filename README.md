<img src="stealth.png">

# Stealth-Continued

A fork of [Stealth](https://gitlab.com/cosmosapps/stealth) by CosmosDev, the account-free,
privacy-oriented Reddit client. The app is still called Stealth and looks the same; under the
hood it talks to one server only: a Teddit API instance you choose, such as
[redlib-tor-api](https://github.com/highda/redlib-tor-api). The phone never contacts Reddit.

## What changed from Stealth 2.3.1

- **One source: the Teddit API.** The Reddit and web scraping sources are gone. Post search,
  user lookup and "load more comments", which Stealth sent to reddit.com even in Teddit mode,
  now go to the instance too.
- **HTTP Basic authentication.** Instances behind a password work: enter the address, the
  username and the password in their own fields. The password is encrypted with a key kept in
  the Android Keystore. Credentials are only ever sent to the instance.
- **Setup wizard.** The first launch asks for the instance and tests the connection.
- **Everything through the instance.** The API, images, videos and downloads share one HTTP
  client, and media URLs from the instance (`/media/...`) get the same credentials.
- **Custom feeds.** Named sets of subreddits per profile, like Reddit's custom feeds. Tap the
  profile avatar on the home screen: Home shows your subscriptions, each feed its subreddits.
  Long-press a feed to edit or delete it; subreddit pages have "Add to a feed".
- **No more "Stealth might stop working" banner** or Reddit API policy dialog.
- Gfycat (shut down in 2023) links open as plain links.
- Changing the instance applies immediately, without a restart.
- Updated to Android 15 (API 35), Gradle 8, Kotlin 1.9 and current libraries.
- Installs next to Stealth (application ID `space.highda.stealth`). To move your
  subscriptions, profiles and saved items, export them in Stealth (Preferences > Import/Export)
  and import them here.

## Instance requirements

Any instance that serves Teddit's API (`?api`, `target=reddit`) works. The app expects:

- the API at the root of the address (`https://api.example.org/r/rust?api`);
- HTTPS with a publicly trusted certificate, or plain HTTP on a network you trust (the app warns
  before sending a password over HTTP);
- optionally HTTP Basic authentication.

With [redlib-tor-api](https://github.com/highda/redlib-tor-api), media is proxied by the
instance as well, so images and videos never come from Reddit's servers either.

### Known instance limitations (redlib-tor-api)

- `/r/a+b` listings come back as r/popular, because the `+` reaches Reddit unencoded. Feeds of
  several subreddits (subscriptions, custom feeds) therefore use a search restricted to those
  subreddits, which has no rising or controversial sort and leaves out stickied posts.
- There is no `/api/morechildren`, so "load more comments" fetches comment permalinks: one
  request for replies, one per comment for top-level comments (8 per tap).
- There is no user search; the Users tab looks the query up as an exact username.

## Features

- Browse Reddit (view posts, comments, subreddits, and users)
- Search posts, subreddits and users
- Subscribe to subreddits
- Custom feeds
- Sort
- History
- Save posts and comments
- Create different profiles (per-profile subscriptions, feeds, history and saved items)
- Show/hide NSFW content
- Image flairs
- Awards
- Save photos and videos
- Light/Dark theme

## Building

JDK 17 and the Android SDK (platform 35) are needed.

```bash
./gradlew assembleDebug
```

Release builds are signed with the key described in `keystore.properties` (not in the
repository):

```properties
storeFile=/path/to/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

## License

Copyright 2021 CosmosDev, and the Stealth-Continued contributors.

Licensed under the GPLv3: http://www.gnu.org/licenses/gpl-3.0.html
