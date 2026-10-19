# Multi-Instance Configuration

How a single app build serves several Open edX platforms ("instances", shown to learners as
Learning Sites): how the feature behaves, and the two sources that configure it, the bundled
config and the remote instance catalog. The schema is the one the iOS app uses, so both apps can
share a catalog.

## How the feature works

An **instance** is one Open edX platform the app can sign in to: its own API host, OAuth client,
branding and per-platform overrides of the app-level configuration.

- **One instance in the resolved catalog**: the app behaves like a classic single-site build. No
  site picker, no Change link on Sign In, no Learning Sites card in Settings. The instance is
  selected automatically. A build with no `INSTANCES` at all also behaves exactly as before.
- **Several instances**: the first screen is Learning Sites. The learner picks a site, can stay
  signed in to more than one, and switches between them from Settings without signing in again.

Selecting a site (`InstanceStore`) reaches every layer below the UI, so nothing leaks between two
sites on the same device:

- **Config**: `InstanceAwareConfig` layers the selected instance over the app-level `Config`. For
  the keys an instance may override (listed below), a value the instance sets wins; anything it
  leaves out comes from the app-level config.
- **Networking**: `InstanceHostInterceptor` re-targets requests built against the app-level
  `API_HOST_URL` at the selected instance (scheme, host and port change; path and query stay).
  Requests to other hosts pass through. The access token is only sent to the selected site, and
  token refresh uses that site's host and `OAUTH_CLIENT_ID`. A response that arrives after a
  switch is handled for the site that sent the request.
- **Preferences**: session and learner data (tokens, user, profile, calendar sync, the server's
  app config) are stored per instance. Device-wide settings (video quality, Wi-Fi only downloads,
  review prompt, What's New) stay shared.
- **Database**: each instance has a Room database file of its own (`OpenEdX_db_<site>`).
- **Downloads**: files go to `instances/<site>/` under the downloads folder. A download that
  finishes after a switch is recorded in the database of the site that queued it.
- **Sessions**: `InstanceSessionManager` switches sites without tearing anything down, and logs
  out of one site without touching the others. Logout keeps that site's cached courses and
  downloaded files, so signing back in is fast; when a different learner signs in to the same
  site, the previous learner's cached course data is cleared.
- **Operations that wait on the network** (sign-in, sign-out, push registration) are pinned to the
  site they started on: their requests carry a `RequestInstance` tag, so they reach that site's
  host with that site's token, and their results are written to that site only. A sign-in that is
  still running when the learner selects another site stops without completing.
- **Push**: the device has one push token; every signed-in site registers it with its backend.
  Signing out of a site unregisters it there, and the token is rotated only once no site is
  signed in.
- **Theming**: the site's accent color, header background and logo apply app-wide when the site
  is selected, at launch and after logout.

On launch the splash screen stays up until the catalog is resolved (see below), so the first
screen always reflects the real catalog.

### Upgrading from a single-site build

Existing data is never handed to a site by its position in the catalog. The instance whose
`API_HOST_URL` (scheme, host, port and path) and `OAUTH_CLIENT_ID` match the app-level values is the
site a single-site build of the app was signing in to, so it keeps the storage that build used:
the unprefixed preferences, `OpenEdX_db` and the existing downloads folder. That decision is
recorded the first time it is made and is not re-derived from later catalogs or configuration
changes. On the first launch after the upgrade, a learner who was signed in lands on that site's
dashboard. If no instance matches, the old data stays where it is and no site can read it.

---

## 1. Bundled config: `config.yaml`

One file per environment, as for every other key (see `ConfigurationManagement.md`). The Gradle
build turns it into the `config.json` the app reads at runtime, so instances are written in YAML
here and arrive as the same JSON shape iOS ships. Unlike iOS, this project keeps YAML as its
source format.

### App-level keys used by the feature

| Key | Purpose |
|---|---|
| `INSTANCES_CATALOG_URL` | URL the remote catalog is fetched from. Empty: only the bundled `INSTANCES` are used and no request is made. |
| `API_HOST_URL`, `OAUTH_CLIENT_ID` | Still required. They are the fallback while no instance is selected, and they identify which instance inherits data from a single-site install. |
| `INSTANCES` | List of instance objects, see below. |

### `INSTANCES[]` schema

Only `NAME`, `API_HOST_URL` (an absolute URL) and `OAUTH_CLIENT_ID` are required. A malformed
entry is dropped, never half-populated. Blank strings count as unset.

| Key | Type | Required | Notes |
|---|---|---|---|
| `KEY` | string | no | Stable identifier; storage is keyed by it. Falls back to a slug of `NAME`. Set it explicitly, and never change it for a live site. |
| `NAME` | string | **yes** | |
| `INSTANCE_NAME` | `{locale: string}` | no | Display name; falls back to `en`, then `NAME`. |
| `API_HOST_URL` | URL string | **yes** | |
| `OAUTH_CLIENT_ID` | string | **yes** | |
| `SSO_URL` | URL string | no | |
| `SSO_FINISHED_URL` / `SSO_URL_SUCCESSFUL_LOGIN` | URL string | no | Either name is accepted. |
| `ENVIRONMENT_DISPLAY_NAME` | string | no | |
| `IS_SWITCH_INSTANCE_LOGIN_ENABLED` | bool | no | Default `false`. Parsed for parity with iOS. |
| `COLOR` | hex string | no | Accent when `THEME` sets none. Default `#007AFF`. |
| `THEME` | object | no | See below. |
| `UI_COMPONENTS` | object | no | Merged per field over the app-level block. May also carry `LOGIN_REGISTRATION_ENABLED`, `SAML_SSO_LOGIN_ENABLED` and `SAML_SSO_DEFAULT_LOGIN_BUTTON`, which override the top-level keys of the same name. |
| `PLATFORM_NAME` | string | no | Defaults to the display name. |
| `TOKEN_TYPE`, `FEEDBACK_EMAIL_ADDRESS`, `FAQ_URL`, `AGREEMENT_URLS`, `DISCOVERY`, `PROGRAM`, `DASHBOARD`, `SSO_BUTTON_TITLE`, `EXPERIMENTAL_FEATURES` | — | no | Same shape as the app-level key; the instance value wins when set. |

Every other key (Firebase, Braze, Branch, social sign-in providers, `URI_SCHEME`, What's New,
pre-login experience) is app-level only; an instance can't override it.

`default_config/dev/config.yaml` contains a complete example entry.

**`THEME`:**

```yaml
THEME:
  LIGHT:
    accent_color: '#3C68FF'
    text_primary: '#18202E'
  DARK:
    accent_color: '#1C355E'
  LOGO_URL: 'https://cdn.example.com/logo.png'
  HEADER_BACKGROUND_URL: 'https://cdn.example.com/header.png'
```

- `LIGHT` / `DARK`: per-field palette overrides. Only the fields set change; a field missing from
  `DARK` uses its `LIGHT` value. The field names are shared with iOS:
  `accent_color`, `accent_x_color`, `alert`, `background`, `card_view_background`,
  `card_view_stroke`, `certificate_foreground`, `text_primary`, `text_secondary`,
  `text_input_background`, `text_input_stroke`, `text_input_text_color`,
  `text_input_placeholder_color`, `warning`, `success`, `info_color`, `primary_button_text_color`,
  `secondary_button_border_color`, `secondary_button_text_color`, `past_due_timeline_color`,
  `today_timeline_color`, `this_week_timeline_color`, `next_week_timeline_color`,
  `upcoming_timeline_color`. Other names are ignored.
- `LOGO_URL` / `HEADER_BACKGROUND_URL`: a remote `http(s)` URL, or the name of a bundled drawable.
  Omit them for the app default. A logo that fails to load shows the site's initial instead.

---

## 2. Remote catalog (fetched from `INSTANCES_CATALOG_URL`)

`OkHttpInstanceApiService` makes a plain GET with an OkHttp client of its own: none of the app's
interceptors, no credentials, no HTTP cache, a 10 second timeout. `InstanceConfigLoader` then
decides what the app runs with:

- Baseline: the last remote catalog that was fetched successfully, else the bundled `INSTANCES`,
  else empty.
- A response with at least one valid instance **replaces** the baseline and becomes the cache.
- A failed, timed-out, malformed or empty response **leaves the baseline untouched**, so a
  reachable but empty catalog never wipes a good cache, and an offline launch shows the catalog
  the learner saw last time.

The baseline is also what a saved selection is restored against at startup, so a process
restored after being killed resolves the same site before any request is made.

### Accepted shapes

A bare array, or an object with the array under `instances` or `INSTANCES`:

```json
[ { "key": "...", "name": "...", "api_host_url": "...", "oauth_client_id": "..." } ]
```
```json
{ "instances": [ { "key": "...", ... } ] }
```

### Keys

The same schema as the bundled entries, parsed by the same model, but expected in `snake_case`
and renamed before parsing. Only the keys below are renamed (at any nesting depth); anything else
passes through **unchanged**, so an unmapped field must be sent in `UPPER_SNAKE_CASE` already:

| Remote key | Becomes |
|---|---|
| `key`, `name`, `color` | `KEY`, `NAME`, `COLOR` |
| `instance_name` | `INSTANCE_NAME` |
| `oauth_client_id` | `OAUTH_CLIENT_ID` |
| `api_host_url` | `API_HOST_URL` |
| `sso_url` | `SSO_URL` |
| `sso_url_successful_login` | `SSO_URL_SUCCESSFUL_LOGIN` |
| `environment_display_name` | `ENVIRONMENT_DISPLAY_NAME` |
| `is_switch_instance_login_enabled` | `IS_SWITCH_INSTANCE_LOGIN_ENABLED` |
| `ui_components` | `UI_COMPONENTS` |
| `course_dropdown_navigation_enabled`, `course_unit_progress_enabled`, `login_registration_enabled`, `saml_sso_login_enabled`, `saml_sso_default_login_button` | Their `UPPER_SNAKE_CASE` names |
| `theme`, `light`, `dark`, `logo_url`, `header_background_url` | `THEME`, `LIGHT`, `DARK`, `LOGO_URL`, `HEADER_BACKGROUND_URL` |

Palette field names inside `LIGHT` / `DARK` (`accent_color`, ...) are never renamed.

### Notes

- The endpoint is public: no auth header is sent, so publish nothing there you wouldn't show
  unauthenticated.
- To test a catalog change, clear the app's data to drop the cache, or serve an empty catalog to
  confirm the app keeps the previous one.
- Removing a site from the catalog hides it; its data stays on the device, and it comes back with
  its session if a later catalog lists it again with the same `KEY`.
