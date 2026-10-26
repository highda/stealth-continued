# 2.7.0 - 2026/10/26

### Added
- Popular (r/popular) and All (r/all) in the profile drawer, next to Home and the custom
  feeds: Reddit's general feeds whatever you subscribe to

### Fixed
- Tapping the text preview of a post in a feed did nothing; the whole card opens the post now
  (links in the preview still open the link)

# 2.6.0 - 2026/10/26

### Added
- Post layout setting (Preferences > UI): Card or Compact, applied to open lists without a
  restart

### Changed
- Card layout (default) in the style of the Reddit app: posts edge to edge with a thin divider,
  one header line (subreddit, author, age), bold titles, full-width media, one compact action
  row. About twice as many posts per screen as before
- Compact layout: small thumbnails on the right, one line of stats, about six posts per screen
- Post screen: less padding around the post, the same style as the Card layout

# 2.5.0 - 2026/10/24

### Added
- User search finds users by name, as on Reddit (on instances with `/users/search`, such as
  redlib-tor-api 0.40.0-tor.2; others still look up the exact username)

### Changed
- Feeds of several subreddits are real `/r/a+b` listings again, with every sort and stickied
  posts (redlib-tor-api 0.40.0-tor.2 fixed the `+`)

### Fixed
- Comments did not load at all with redlib-tor-api 0.40.0-tor.1 (Reddit's GraphQL comments)
- "Load more comments" with redlib-tor-api: one request per tap, through Reddit's continuation
  cursors, for replies and for the next top-level comments

# 2.4.0 - 2026/10/14

First release of Stealth-Continued, a fork of Stealth 2.3.1.

### Added
- HTTP Basic authentication for the Teddit API instance (password encrypted on the device)
- Setup wizard on first launch, with a connection test
- Custom feeds: named sets of subreddits per profile, in the profile drawer
- "Add to a feed" on subreddit pages
- Custom feeds in backups
- A notice when NSFW results are hidden, with a button to show them

### Changed
- The Teddit API is the only source; the Reddit and web scraping sources are gone
- Post search, user lookup and "load more comments" go to the instance instead of reddit.com
- Images, videos and downloads use the same HTTP client as the API
- Changing the instance applies without a restart
- Feeds of several subreddits use a restricted search (redlib-tor-api answers /r/a+b with r/popular)
- The Privacy Enhancer's instance lists are current (from LibRedirect)
- Android 15 (API 35), Gradle 8, Kotlin 1.9 and current libraries
- Application ID space.highda.stealth: installs next to Stealth

### Removed
- "Stealth might stop working" banner and the Reddit API policy dialog
- Gfycat (shut down in 2023): its links open as plain links

### Fixed
- Comments that fail to load are no longer dropped from "More"
- Content scrolling visibly under the status bar on Android 15+

# 2.3.1 - 2025/06/01

### Fixed
- Redgifs videos

# 2.3.0 - 2023/08/15

This version is only compatible with Android 6.0 onwards.

### Added
- Monochrome icon

### Changed
- Improve NSFW consent when scraping
- Minimum Android version (now Android 6.0)

### Fixed
- App not starting on Android 14
- Email intent on About page

# 2.2.2 - 2023/07/30

### Fixed
- Missing audio in Reddit videos

# 2.2.1 - 2023/06/05

### Fixed
- Missing ratio in post details

# 2.2.0 - 2023/06/04

### Added
- Web Scraping source (based on old.reddit.com)
- Disclaimer for Reddit's API policy change

# 2.1.1 - 2023/03/18

### Added
- More Teddit API endpoints

### Fixed
- Redgifs videos

# 2.1.0 - 2022/12/28

### Added
- Privacy Enhancer: redirect services to privacy-friendly frontends
- More Teddit instances

### Changed
- Automatically upgrade media links to HTTPS

### Fixed
- Various bugs

# 2.0.3 - 2022/12/13

### Fixed
- Fix unselectable backup files when using the import feature

# 2.0.2 - 2022/12/10

### Fixed
- Crashes with blank profile names

# 2.0.1 - 2022/11/28

### Changed
- Better sorting when having more than 100 subscriptions

### Fixed
- Endless loading in case of errors
- Padding on home screen

# 2.0.0 - 2022/11/26

This includes changes from previous alpha versions.

### Added
- Teddit API: choose between Reddit official API and Teddit API to fetch data (experimental)
- Amoled theme
- Import/Export feature: back up and restore data from Stealth or Reddit
- Profile Switcher on home screen
- Pull to refresh
- Support for more than 100 subscriptions

### Changed
- Improved performances
- Various UI improvements

### Removed
- In-app browser: replace with CustomTab

### Fixed
- Some crashes and UI bugs

# 2.0.0-alpha04 - 2022/09/10

### Added
- Support for body text in all posts
- Log crashes to a file on external storage

### Changed
- Moved bottom navigation bar to the side (with left-handed support)

### Fixed
- Redgifs videos (temporary fix)
- Crashes when domain or url is null in a post

# 2.0.0-alpha03 - 2022/04/17

### Fixed
- Crashes when restoring backups

# 2.0.0-alpha02 - 2022/04/17

### Added
- Import/Export feature: back up and restore data from Stealth or Reddit

### Fixed
- Discrepancies in comments when leaving and coming back to post detail
- Bottom navigation not hiding on scroll in Post List
- Crashes when clicking on poll links (thanks to @mwiggins)

# 2.0.0-alpha01 - 2021/12/13

### Added
- Teddit API: choose between Reddit official API and Teddit API to fetch data (experimental)
- Amoled theme
- Incognito mode by default for keyboard

### Changed
- Improved performances
- Various UI updates

### Removed
- In-app browser: replace with CustomTab
- Max limit for search queries

### Fixed
- Unordered images in galleries
- Comments not saved on configuration change
- Banner not collapsing when navigating to/from Subreddits

# 1.1.3 - 2021/10/25

### Fixed
- Crashes on Android 12+

# 1.1.2 - 2021/08/28

### Added
- Support for redd.it links

### Fixed
- Redgifs videos
- Reddit permalinks opened as external links

# 1.1.1 - 2021/08/19

### Added
- Support for physical keyboards

### Fixed
- NSFW content not showing in search results
- Reddit galleries not loading in saved posts
- Crashes with some user and Subreddit links

# 1.1.0 - 2021/07/17

### Added
- Built-in browser
- Media downloader: save images and videos from posts
- Ability to rename profiles

### Changed
- Improve navigation

### Fixed
- Crashes on some Subreddits
- Wiki links opened as Subreddit links

# 1.0.1 - 2021/05/09

### Fixed
- Fix external links on Android 11+

# 1.0.0 - 2021/05/09

### Added
- Profiles: create different profiles to have distinct subscriptions, history and saved items
- Support for crossposts
- Ability to save posts and comments
- Suggested sort
- Mute button in Media Viewer
- Clickable links in tables
- German translation (thanks to @uDEV2019)
- Spanish translation (thanks to @another-sapiens)

### Changed
- Open drawer more easily on a Subreddit page

# 0.1.1 - 2021/04/06

### Fixed
- Fix Reddit videos not playing when audio is not found
- Fix crash when opening Subreddit from user page
- Fix share feature
- Fix wrong search query being displayed in search page
- Fix for some Gfycat videos not playing

# 0.1.0 - 2021/03/27

### Added
- Subscriptions
- Search
- Sort
- History
- NSFW filter
- Flairs
- Awards
- Light/Dark theme
