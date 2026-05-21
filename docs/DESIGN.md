# UPI Payment Speaker — UI/UX Design Specification

> **Purpose**: This document is the definitive design reference for building production-ready UI screens for the UPI Payment Speaker Android app. It covers every screen, component, interaction, and design token. Use this to produce complete, shippable designs — not wireframes or prototypes.

---

## 1. Product Context

### What the app does
An Android app for **Indian shopkeepers and merchants** that **speaks UPI payment amounts aloud via TTS** when a payment notification arrives (PhonePe, GPay, Paytm, etc.) — even when the screen is off or the phone is in a pocket/drawer.

### Target users
- Small shop owners, street vendors, kirana store merchants
- Age range: 25–60, many not tech-savvy
- Often operate in noisy environments (market, traffic)
- Phone is usually in a pocket/drawer, not in hand
- May be literate only in their regional language (Kannada, Hindi, etc.)
- Need **instant, glanceable** confirmation that money arrived

### Core UX principles
1. **Glanceable** — Information must be readable in < 2 seconds
2. **Accessible** — Large text, high contrast, minimal reading required
3. **Trustworthy** — The app must look and feel reliable (merchants' livelihood depends on it)
4. **Minimal interaction** — Set it up once, then forget it. The home screen is a status dashboard, not a control panel
5. **Multilingual** — UI text must support language switching (English, Kannada initially; Hindi, Marathi, Tamil, Telugu, Bengali planned)

---

## 2. App Architecture (Screens & Navigation)

### Screen inventory

| # | Screen | Type | Purpose |
|---|--------|------|---------|
| 1 | **Onboarding** | Full-screen wizard (multi-step) | First-run permission setup |
| 2 | **Home** | Main dashboard | Status indicator + recent transactions |
| 3 | **Transaction History** | Full-screen list | Complete transaction log with filters |
| 4 | **Settings** | Full-screen | TTS, language, volume, popup, providers |
| 5 | **Payment Popup** | Overlay (translucent) | Brief payment confirmation on screen |

### Navigation structure

```
┌─────────────────────────────────────┐
│           Bottom Nav Bar            │
│  ┌─────┐   ┌─────────┐   ┌──────┐  │
│  │Home │   │ History  │   │ Set- │  │
│  │ 🏠  │   │   📋    │   │tings │  │
│  │     │   │         │   │  ⚙️  │  │
│  └─────┘   └─────────┘   └──────┘  │
└─────────────────────────────────────┘
```

- **3 tabs**: Home, History, Settings
- Bottom navigation bar (Material Design `BottomNavigationView`)
- No drawer, no hamburger menu — keep it simple for the target audience
- Payment Popup is an overlay that appears on top of any screen (or lock screen)
- Onboarding is a separate flow shown only on first launch (or when permissions regress)

---

## 3. Design Tokens

### 3.1 Color palette (current — dark theme)

| Token | Hex | Usage |
|-------|-----|-------|
| `background` | `#0E1014` | App background, status bar, nav bar |
| `card_surface` | `#1A1D24` | Cards, elevated surfaces, bottom nav |
| `text_on_bg` | `#FFFFFF` | Primary text on background |
| `text_secondary` | `#B0B6C0` | Subtitle text, timestamps, hints |
| `primary` | `#5BD0AA` | Primary actions, active states, status "Listening" |
| `primary_dark` | `#3FA084` | Pressed state of primary |
| `on_primary` | `#0E1014` | Text on primary-colored surfaces |
| `accent` | `#FFC857` | Accent highlights, amounts, attention |
| `warn` | `#E36161` | Error banners, permission warnings |
| `success` | `#5BD0AA` | Same as primary — payment received confirmation |
| `divider` | `#2A2D35` | Subtle separators between list items |

### 3.2 Typography

| Style | Size | Weight | Usage |
|-------|------|--------|-------|
| Display | 48sp | Bold | Payment amount in popup |
| Headline 1 | 32sp | Bold | Home screen status text |
| Headline 2 | 24sp | Bold | Section headers, onboarding titles |
| Title | 20sp | Semi-bold | Card titles, settings group headers |
| Body 1 | 16sp | Regular | Body text, settings descriptions, onboarding body |
| Body 2 | 14sp | Regular | Transaction list items, secondary info |
| Caption | 12sp | Regular | Timestamps, labels, step indicators |
| Amount (large) | 48sp | Bold | Payment popup amount |
| Amount (list) | 18sp | Semi-bold | Transaction list amount |

- Default system font (Roboto) for English
- Noto Sans Kannada for Kannada text (bundled with Android)
- Numbers should always be large and high-contrast

### 3.3 Spacing & Layout

| Token | Value | Usage |
|-------|-------|-------|
| Screen padding | 20dp | Horizontal page margins |
| Card padding | 16dp | Internal card padding |
| Card corner radius | 16dp | Rounded cards |
| Card elevation | 2dp | Subtle lift |
| Item spacing (vertical) | 12dp | Between list items |
| Section spacing | 24dp | Between sections on a page |
| Button height | 52dp | All primary/secondary buttons |
| Button corner radius | 12dp | Rounded buttons |
| Bottom nav height | 64dp | Standard Material bottom nav |
| Icon size (nav) | 24dp | Bottom nav icons |
| Icon size (inline) | 20dp | Icons inside list items |

### 3.4 Iconography
- Use **Material Symbols Outlined** (Google's icon set) for consistency
- Key icons needed:
  - Home tab: `home` or `dashboard`
  - History tab: `receipt_long` or `history`
  - Settings tab: `settings`
  - Listening state: `graphic_eq` (sound wave) or `hearing`
  - Not listening: `hearing_disabled`
  - Payment received: `payments` or `check_circle`
  - Warning: `warning`
  - PhonePe: custom or generic `account_balance_wallet`
  - Language: `translate`
  - Volume: `volume_up`
  - TTS mode: `record_voice_over`

---

## 4. Screen Designs

### 4.1 Onboarding Wizard

**When shown**: First app launch, or when the watchdog detects a permission has been revoked.

**Flow**:
```
Step 1: Welcome
   ↓
Step 2: Grant notification posting permission (Android 13+)
   ↓
Step 3: Enable Notification Listener access
   ↓
Step 4: Allow exact alarms
   ↓
Step 5: Disable battery optimisation
   ↓
Step 6: OEM-specific steps (Vivo / Xiaomi / Oppo / Samsung — auto-detected)
   ↓
Step 7: Test announcement ("Two hundred and fifty rupees received")
   ↓
→ Home screen
```

**Layout per step**:
```
┌────────────────────────────────┐
│  Step 3 of 7              [Skip]│
│                                │
│  ┌────────────────────────┐    │
│  │                        │    │
│  │    [Illustration /     │    │
│  │     Lottie animation]  │    │
│  │                        │    │
│  └────────────────────────┘    │
│                                │
│  Allow notification access     │
│  ────────────────────────      │
│  We read transaction           │
│  notifications from your UPI   │
│  apps (PhonePe, Google Pay,    │
│  Paytm) and speak the amount.  │
│  We never send anything off    │
│  the device.                   │
│                                │
│  ┌────────────────────────┐    │
│  │   Grant Permission     │    │
│  └────────────────────────┘    │
│                                │
│  ● ● ◉ ○ ○ ○ ○    progress    │
└────────────────────────────────┘
```

**Design details**:
- Full-screen, no bottom nav during onboarding
- Progress indicator: dots at the bottom (filled = done, outlined = current, empty = upcoming)
- Each step has an optional illustration/icon area (top 40% of screen)
- Title: 24sp bold
- Body: 16sp regular, `text_secondary` color
- Primary button: full width, `primary` color, 52dp height
- "Skip" button: text-only, top-right corner (for optional steps)
- Step indicator: "Step X of Y" caption at top-left
- Steps that are already granted are auto-skipped

**Welcome step special layout**:
- Large app icon/logo centered
- App name "UPI Payment Speaker" as title
- Tagline: "Hear every UPI payment" as subtitle
- Brief description of what the app does
- "Get Started" button

**Test announcement step special layout**:
- Large speaker/sound-wave icon or animation
- "Tap to test" button that triggers test TTS
- Visual feedback: waveform animation or pulsing icon while speaking
- Success: green checkmark + "You should have heard the announcement"

---

### 4.2 Home Screen (Main Dashboard)

The primary screen users see daily. Must communicate "the app is working" at a glance.

```
┌────────────────────────────────┐
│                                │
│   ┌────────────────────────┐   │
│   │                        │   │
│   │   ╭───────────────╮    │   │
│   │   │  )))  Speaker │    │   │
│   │   │    icon with   │   │   │
│   │   │   pulse anim   │   │   │
│   │   ╰───────────────╯    │   │
│   │                        │   │
│   │     Listening           │   │
│   │                        │   │
│   │   ₹12,500 received     │   │
│   │   today • 8 payments    │   │
│   │                        │   │
│   └────────────────────────┘   │
│                                │
│   ⚠ Battery optimisation is   │
│   on — tap to fix.            │
│                                │
│   ┌────────────────────────┐   │
│   │ Test Announcement      │   │
│   └────────────────────────┘   │
│                                │
│   Recent Transactions          │
│   ─────────────────────────    │
│   ┌────────────────────────┐   │
│   │ ₹250      PhonePe     │   │
│   │ 2:34 PM   Spoken ✓    │   │
│   ├────────────────────────┤   │
│   │ ₹1,000    PhonePe     │   │
│   │ 1:15 PM   Spoken ✓    │   │
│   ├────────────────────────┤   │
│   │ ₹50       GPay        │   │
│   │ 12:02 PM  Spoken ✓    │   │
│   └────────────────────────┘   │
│   See all →                    │
│                                │
│ ┌──────┬──────────┬──────────┐ │
│ │ Home │ History  │ Settings │ │
│ └──────┴──────────┴──────────┘ │
└────────────────────────────────┘
```

**Components**:

#### Status card (hero area, top)
- Large visual indicator of listening state:
  - **Listening**: Animated sound-wave or pulsing speaker icon, `primary` green color, text "Listening"
  - **Paused/Muted**: Static muted speaker icon, `text_secondary` gray, text "Paused"
  - **Setup needed**: Warning icon, `warn` red, text "Setup needed" with a "Fix" button
- Below the status: today's summary stats
  - "₹12,500 received today" — amount in `accent` color, large text
  - "8 payments" — secondary text
- The entire card is a rounded surface (`card_surface` background, 16dp radius)
- Tapping the status area toggles listening on/off

#### Warning banner (conditional)
- Appears below the status card ONLY when a permission is revoked
- Background: `warn` color at 15% opacity, `warn` border-left 4dp
- Icon: `warning` in `warn` color
- Text: description of issue (e.g., "Battery optimisation is on — announcements may be missed.")
- "Tap to fix" action — opens the relevant system setting
- Multiple warnings stack vertically

#### Test button
- Outlined button, full width
- "Test Announcement" — triggers a sample TTS utterance
- While speaking: shows a small waveform animation on the button

#### Recent transactions (mini-list)
- Section header: "Recent Transactions" with a "See all →" link to History tab
- Shows last 3–5 transactions
- Each row:
  - Left: Amount in `accent` or `text_on_bg` color, 18sp semi-bold (e.g., "₹250")
  - Right: Source app name (e.g., "PhonePe") + status icon
  - Below: Timestamp in `text_secondary`, 12sp
  - Status: small icon — green check for spoken, red X for failed, gray clock for duplicate-skipped
- Empty state: Illustration + "No transactions yet — make a small test payment to verify."

---

### 4.3 Transaction History Screen

Full transaction log with filtering.

```
┌────────────────────────────────┐
│  Transaction History           │
│                                │
│  ┌──────┬──────┬──────┬─────┐  │
│  │ All  │Today │ Week │Month│  │
│  └──────┴──────┴──────┴─────┘  │
│                                │
│  Today, 20 May 2026            │
│  ─────────────────────────     │
│  ┌────────────────────────┐    │
│  │  ↓  ₹250.00            │    │
│  │  PhonePe • 2:34 PM     │    │
│  │  Status: Spoken ✓      │    │
│  ├────────────────────────┤    │
│  │  ↓  ₹1,000.00          │    │
│  │  PhonePe • 1:15 PM     │    │
│  │  Status: Spoken ✓      │    │
│  ├────────────────────────┤    │
│  │  ↓  ₹50.00             │    │
│  │  GPay • 12:02 PM       │    │
│  │  Status: Spoken ✓      │    │
│  └────────────────────────┘    │
│                                │
│  Yesterday, 19 May 2026        │
│  ─────────────────────────     │
│  ┌────────────────────────┐    │
│  │  ↓  ₹5,000.00          │    │
│  │  PhonePe • 4:50 PM     │    │
│  │  Status: Spoken ✓      │    │
│  └────────────────────────┘    │
│                                │
│ ┌──────┬──────────┬──────────┐ │
│ │ Home │ History  │ Settings │ │
│ └──────┴──────────┴──────────┘ │
└────────────────────────────────┘
```

**Components**:

#### Filter chips (top)
- Horizontal scrollable chip row: All, Today, This Week, This Month
- Active chip: `primary` background, `on_primary` text
- Inactive chip: `card_surface` background, `text_secondary` text
- Optional: Provider filter (All, PhonePe, GPay, Paytm)

#### Date group headers
- Sticky section headers: "Today, 20 May 2026", "Yesterday", then dates
- `text_secondary` color, 14sp, with a subtle divider below

#### Transaction card/row
- Each transaction is a card or list row:
  - **Direction icon**: Down-arrow (↓) in `primary` green for credit, up-arrow (↑) in `warn` red for debit
  - **Amount**: ₹250.00 in large text (18sp semi-bold)
  - **Source + time**: "PhonePe • 2:34 PM" in `text_secondary`
  - **Status**: Small badge/icon
    - "Spoken" — green check
    - "Failed" — red X with reason on tap
    - "Duplicate" — gray "skipped" label
- No swipe-to-delete (transaction log is for audit; should not be deletable)

#### Empty state
- Centered illustration
- "No transactions recorded yet"
- "Payments will appear here once the app announces them"

---

### 4.4 Settings Screen

All user-configurable options.

```
┌────────────────────────────────┐
│  Settings                      │
│                                │
│  ANNOUNCEMENT                  │
│  ┌────────────────────────┐    │
│  │ Announcement mode       │    │
│  │ Single language     [>] │    │
│  ├────────────────────────┤    │
│  │ Primary language        │    │
│  │ English (India)     [>] │    │
│  ├────────────────────────┤    │
│  │ Second language         │    │
│  │ ಕನ್ನಡ (Kannada)     [>] │    │
│  │ (Only in Dual mode)     │    │
│  └────────────────────────┘    │
│                                │
│  SOUND                         │
│  ┌────────────────────────┐    │
│  │ Chime sound             │    │
│  │ Classic             [>] │    │
│  ├────────────────────────┤    │
│  │ Chime for large     [⊙] │    │
│  │ amounts                 │    │
│  ├────────────────────────┤    │
│  │ Use alarm volume    [⊙] │    │
│  │ Plays at max volume     │    │
│  ├────────────────────────┤    │
│  │ Announce debits     [ ] │    │
│  └────────────────────────┘    │
│                                │
│  DISPLAY                       │
│  ┌────────────────────────┐    │
│  │ Payment popup       [⊙] │    │
│  │ Show amount on screen   │    │
│  └────────────────────────┘    │
│                                │
│  SECURITY                      │
│  ┌────────────────────────┐    │
│  │ Fake payment        [⊙] │    │
│  │ detection               │    │
│  ├────────────────────────┤    │
│  │ Alert sensitivity       │    │
│  │ Medium             [>]  │    │
│  └────────────────────────┘    │
│                                │
│  DAILY SUMMARY                 │
│  ┌────────────────────────┐    │
│  │ Daily summary       [⊙] │    │
│  ├────────────────────────┤    │
│  │ Summary time            │    │
│  │ 9:00 PM            [>]  │    │
│  ├────────────────────────┤    │
│  │ Speak summary       [⊙] │    │
│  └────────────────────────┘    │
│                                │
│  APP LANGUAGE                  │
│  ┌────────────────────────┐    │
│  │ App language            │    │
│  │ English             [>] │    │
│  └────────────────────────┘    │
│                                │
│  PROVIDERS                     │
│  ┌────────────────────────┐    │
│  │ PhonePe            [⊙]  │    │
│  │ Google Pay          [ ]  │    │
│  │ Paytm              [ ]  │    │
│  │ BHIM               [ ]  │    │
│  └────────────────────────┘    │
│                                │
│  PERMISSIONS                   │
│  ┌────────────────────────┐    │
│  │ Re-run setup wizard [>] │    │
│  └────────────────────────┘    │
│                                │
│  ABOUT                         │
│  ┌────────────────────────┐    │
│  │ Version 1.0.0           │    │
│  │ Privacy Policy      [>] │    │
│  └────────────────────────┘    │
│                                │
│ ┌──────┬──────────┬──────────┐ │
│ │ Home │ History  │ Settings │ │
│ └──────┴──────────┴──────────┘ │
└────────────────────────────────┘
```

**Sections & controls**:

#### ANNOUNCEMENT section
| Setting | Control type | Options | Default |
|---------|-------------|---------|---------|
| Announcement mode | Dropdown or radio | Single / Dual (speak in 2 languages one after another) | Single |
| Primary language | Dropdown | English (India), ಕನ್ನಡ (Kannada), हिन्दी (Hindi)* | English (India) |
| Second language | Dropdown (disabled when mode=Single) | Same options as primary | None |
| Shake to repeat | Dropdown | Off / Low / Medium / High sensitivity | Medium |

*Hindi and other languages are shown but grayed out with "Coming soon" if not yet implemented.

#### SOUND section
| Setting | Control type | Notes | Default |
|---------|-------------|-------|---------|
| Chime sound | Dropdown (with preview) | Classic / Cash register / Digital / Coin drop / Custom / None | Classic |
| Chime for large amounts | Toggle switch | "Play extra chimes for ₹1,000+" | ON |
| Use alarm volume | Toggle switch | "Plays at maximum volume using alarm stream" | ON |
| Announce debits | Toggle switch | "Also speak when you send money" | OFF |

#### DISPLAY section
| Setting | Control type | Notes | Default |
|---------|-------------|-------|---------|
| Payment popup | Toggle switch | "Show payment amount on screen when received" | ON |

#### SECURITY section
| Setting | Control type | Notes | Default |
|---------|-------------|-------|---------|
| Fake payment detection | Toggle switch | "Warn about suspicious notifications" | ON |
| Alert sensitivity | Dropdown | Low / Medium / High | Medium |

#### DAILY SUMMARY section
| Setting | Control type | Notes | Default |
|---------|-------------|-------|---------|
| Daily summary | Toggle switch | "Show end-of-day payment summary" | ON |
| Summary time | Time picker | "When to show/speak the daily summary" | 9:00 PM |
| Speak summary | Toggle switch | "Read summary aloud at summary time" | ON |

#### APP LANGUAGE section
| Setting | Control type | Notes | Default |
|---------|-------------|-------|---------|
| App language | Dropdown | Changes UI text language (English, ಕನ್ನಡ) | English |

#### PROVIDERS section
| Setting | Control type | Notes | Default |
|---------|-------------|-------|---------|
| PhonePe | Toggle switch | Listen to PhonePe notifications | ON |
| Google Pay | Toggle switch | | OFF (not yet implemented) |
| Paytm | Toggle switch | | OFF (not yet implemented) |
| BHIM | Toggle switch | | OFF (not yet implemented) |

Unimplemented providers show as disabled with "Coming soon" label.

#### PERMISSIONS section
- "Re-run setup wizard" — opens the onboarding wizard
- Shows current permission status with green/red indicators

#### ABOUT section
- Version number
- Privacy policy link
- "Made with ♥ in India" (optional)

**Design details**:
- Group settings under section headers (all caps, `text_secondary`, 12sp)
- Each setting row: 56dp height minimum
- Label on left, control on right
- Description text below label in `text_secondary`, 12sp
- Toggle switches use `primary` color when ON
- Dropdowns open a bottom sheet with radio options
- Cards with `card_surface` background, grouped settings inside

---

### 4.5 Payment Popup (Overlay)

Appears over any screen (including lock screen) when a payment is received. Auto-dismisses after 4 seconds.

```
┌────────────────────────────────┐
│                                │
│        (dimmed background)     │
│                                │
│    ┌──────────────────────┐    │
│    │                      │    │
│    │   Payment Received   │    │
│    │                      │    │
│    │      ₹250            │    │
│    │                      │    │
│    │  two hundred and     │    │
│    │  fifty rupees        │    │
│    │                      │    │
│    │  via PhonePe         │    │
│    │                      │    │
│    │  ──── progress bar ──│    │
│    │                      │    │
│    └──────────────────────┘    │
│                                │
└────────────────────────────────┘
```

**Design details**:
- Background: dimmed (60% black overlay behind the popup card)
- Popup card: `card_surface` (#1A1D24) with 87% opacity, 24dp corner radius
- Card padding: 32dp horizontal, 40dp vertical
- Content centered vertically and horizontally in the card

**Content layout (top to bottom)**:
1. **Label**: "Payment Received" — 20sp, `primary` (#5BD0AA) color
2. **Amount**: "₹250" — 48sp bold, white (#FFFFFF)
3. **Amount in words**: "two hundred and fifty rupees" — 16sp, `text_secondary` (#B0B6C0)
4. **Source** (optional): "via PhonePe" — 14sp, `text_secondary`
5. **Auto-dismiss progress bar**: thin (3dp) horizontal bar at the bottom of the card, `primary` color, animates from full width to zero over 4 seconds

**Behavior**:
- Appears with a slide-up + fade-in animation (200ms)
- Dismisses with fade-out (150ms)
- Auto-dismiss after 4 seconds
- Tapping anywhere on the popup dismisses it immediately
- Works over lock screen (turns screen on)
- If multiple payments arrive in quick succession, queue them (show one at a time)

**Amount display rules**:
- Always show ₹ symbol before the amount
- Use Indian comma formatting: ₹1,00,000 (not ₹100,000)
- Show paise only if non-zero: ₹250 (not ₹250.00), but ₹250.50
- Amount in words uses the selected primary TTS language:
  - English: "two hundred and fifty rupees"
  - Kannada: "ಇನ್ನೂರ ಐವತ್ತು ರೂಪಾಯಿ"

---

### 4.6 Repeat Last Announcement

A quick way for the merchant to re-hear the last payment when they missed it in a noisy environment.

**Trigger methods**:
1. **Floating Action Button (FAB)** on Home screen — always visible
2. **Shake gesture** — shake the phone to replay (configurable)
3. **Notification action** — "Repeat" button on the persistent foreground notification

**FAB on Home screen**:
```
┌────────────────────────────────┐
│                                │
│   [ Status card ... ]          │
│   [ Recent transactions ... ]  │
│                                │
│                          ┌───┐ │
│                          │ 🔁│ │
│                          └───┘ │
│                                │
│ ┌──────┬──────────┬──────────┐ │
│ │ Home │ History  │ Settings │ │
│ └──────┴──────────┴──────────┘ │
└────────────────────────────────┘
```

**FAB design**:
- Position: bottom-right, 16dp margin from edges, above bottom nav
- Size: 56dp (standard FAB)
- Background: `accent` (#FFC857)
- Icon: `replay` material icon, `on_primary` (#0E1014) color
- Elevation: 6dp
- Shape: circular (28dp radius)

**Behavior**:
- Tapping replays the last announced payment via TTS (same language, same volume settings)
- If no payments have been announced yet, show a brief toast: "No announcements yet"
- Brief haptic feedback on tap
- While replaying: FAB icon changes to animated sound-wave, returns to replay icon when done
- Shake detection sensitivity configurable in Settings (Off / Low / Medium / High)
- Shake gesture works even when screen is off (via accelerometer sensor in foreground service)

**Notification action**:
- Add a "Repeat" action button to the existing persistent foreground notification
- Icon: `replay` material icon
- Triggers the same TTS replay as the FAB

**Settings additions**:
| Setting | Control type | Options | Default | Section |
|---------|-------------|---------|---------|---------|
| Shake to repeat | Dropdown | Off / Low / Medium / High sensitivity | Medium | ANNOUNCEMENT |

---

### 4.7 Distinct Chime Before TTS

A short audio chime/tone that plays before the TTS announcement begins, to grab attention in noisy environments.

**How it works**:
1. Payment notification arrives
2. Chime plays (200–500ms)
3. Brief pause (300ms)
4. TTS speaks the amount
5. (If dual-TTS: brief pause → TTS speaks in second language)

**Chime options**:
| Chime | Description | Duration | Use case |
|-------|------------|----------|----------|
| Classic | Short "ting" bell sound | 300ms | Default — clean, professional |
| Cash register | "Ka-ching" register sound | 400ms | Fun, shop-appropriate |
| Digital | Short electronic beep | 200ms | Minimal, unobtrusive |
| Coin drop | Metallic coin sound | 350ms | Traditional payment feel |
| Custom | User-selected from device | Variable | Personalisation |
| None | No chime | 0ms | TTS only |

**Amount-based chime intensity** (optional, configurable):
| Amount range | Behavior |
|-------------|----------|
| < ₹100 | Single chime, normal volume |
| ₹100 – ₹999 | Single chime, normal volume |
| ₹1,000 – ₹9,999 | Double chime (play twice quickly) |
| ₹10,000+ | Triple chime — signals "big payment!" |

**Settings additions**:
| Setting | Control type | Options | Default | Section |
|---------|-------------|---------|---------|---------|
| Chime sound | Dropdown (with preview) | Classic / Cash register / Digital / Coin drop / Custom / None | Classic | SOUND |
| Chime for large amounts | Toggle switch | "Play extra chimes for ₹1,000+" | ON | SOUND |

**Chime picker UI** (bottom sheet):
```
┌────────────────────────────────┐
│  Select chime sound            │
│  ─────────────────────────     │
│  ┌────────────────────────┐    │
│  │ ◉ Classic          [▶] │    │
│  │ ○ Cash register    [▶] │    │
│  │ ○ Digital          [▶] │    │
│  │ ○ Coin drop        [▶] │    │
│  │ ○ Custom file...   [▶] │    │
│  │ ○ None                  │    │
│  └────────────────────────┘    │
│                                │
│  ┌────────────────────────┐    │
│  │       Done              │    │
│  └────────────────────────┘    │
└────────────────────────────────┘
```

- Each option has a play button [▶] that previews the sound
- "Custom file..." opens the system file picker for audio files
- Radio selection (single choice)
- `card_surface` background, `primary` for selected radio

**Technical notes**:
- Chime audio files bundled as raw resources (`res/raw/chime_classic.ogg`, etc.)
- Played via `MediaPlayer` or `SoundPool` on `STREAM_ALARM` (same stream as TTS)
- Must respect the same volume settings as TTS
- Custom chime: stored in app's internal storage, max 2 seconds, validated on import

---

### 4.8 Daily Summary

Automatic end-of-day summary of all payments received — spoken aloud and/or shown as a notification.

**Summary notification** (at configurable time, e.g., 9 PM):
```
┌────────────────────────────────┐
│ 📊 Daily Summary — 20 May      │
│ ₹45,200 received • 23 payments │
│ Tap to view details             │
└────────────────────────────────┘
```

**Summary screen** (accessible from Home or notification tap):
```
┌────────────────────────────────┐
│  ← Daily Summary               │
│                                │
│  ┌────────────────────────┐    │
│  │                        │    │
│  │   Today, 20 May 2026   │    │
│  │                        │    │
│  │     ₹45,200            │    │
│  │   total received        │    │
│  │                        │    │
│  │   23 payments           │    │
│  │                        │    │
│  └────────────────────────┘    │
│                                │
│  Breakdown by app              │
│  ─────────────────────────     │
│  ┌────────────────────────┐    │
│  │ PhonePe      ₹32,500   │    │
│  │              18 payments│    │
│  ├────────────────────────┤    │
│  │ Google Pay   ₹10,200   │    │
│  │              4 payments │    │
│  ├────────────────────────┤    │
│  │ Paytm        ₹2,500    │    │
│  │              1 payment  │    │
│  └────────────────────────┘    │
│                                │
│  Largest payment: ₹5,000       │
│  Smallest payment: ₹20         │
│  Average: ₹1,965               │
│                                │
│  ┌────────────────────────┐    │
│  │  Share Summary          │    │
│  └────────────────────────┘    │
│                                │
└────────────────────────────────┘
```

**Design details**:

#### Summary hero card
- Background: `card_surface`
- Date: 16sp, `text_secondary`
- Total amount: 40sp bold, `accent` (#FFC857) — stands out as the most important number
- "total received" label: 14sp, `text_secondary`
- Payment count: 20sp semi-bold, `text_on_bg`
- Corner radius: 16dp

#### Breakdown by app
- Each provider row: app name on left, amount + count on right
- Sorted by total amount (highest first)
- Provider icon if available, fallback to generic wallet icon

#### Stats footer
- Largest, smallest, average — in `text_secondary`, 14sp
- Displayed as simple label: value pairs

#### Share button
- Outlined button, full width
- Generates a shareable text summary:
  ```
  📊 Daily Summary — 20 May 2026
  Total: ₹45,200 (23 payments)
  PhonePe: ₹32,500 (18)
  Google Pay: ₹10,200 (4)
  Paytm: ₹2,500 (1)
  — UPI Payment Speaker
  ```
- Share via Android share sheet (WhatsApp, SMS, etc.)

**TTS summary announcement** (optional):
- At the configured time, the app speaks: "Today's summary. Forty-five thousand two hundred rupees received across twenty-three payments."
- Uses the primary TTS language
- Plays after the chime (if chime is enabled)

**Settings additions**:
| Setting | Control type | Options | Default | Section |
|---------|-------------|---------|---------|---------|
| Daily summary | Toggle switch | Enable/disable | ON | ANNOUNCEMENT |
| Summary time | Time picker | Any time | 9:00 PM | ANNOUNCEMENT |
| Speak summary | Toggle switch | "Read summary aloud at summary time" | ON | ANNOUNCEMENT |

**Historical summaries**:
- Accessible from History tab → "Daily Summaries" filter chip
- Shows a list of past daily summary cards (scrollable)
- Each card: date, total amount, payment count
- Tap to expand to full summary view

---

### 4.9 Fake Payment Detection

Alerts the merchant when a payment notification appears suspicious or doesn't match the expected pattern of a genuine UPI payment app.

**How it works**:

#### Detection layers

| Layer | What it checks | Confidence |
|-------|---------------|------------|
| **Package verification** | Is the notification from the real PhonePe/GPay/Paytm package? Fake apps use different package names. | High |
| **Notification structure** | Does the notification have the expected extras structure (bigText, title format, category)? Fake notifications from screen-sharing/overlay apps often have malformed extras. | Medium |
| **Pattern mismatch** | Does the text match known genuine patterns? Missing ₹ symbol, unusual phrasing, or text that's close-but-not-exact to real notifications. | Medium |
| **Rapid duplicate** | Same amount from same "sender" within seconds? Could be a replayed notification. | Low-Medium |
| **Screen overlay detection** | Is another app drawing over the screen while showing a "payment" (common scam technique)? | High |

#### Alert UI — Suspicious Payment Warning

When a suspicious notification is detected, instead of (or alongside) the normal payment popup:

```
┌────────────────────────────────┐
│                                │
│        (dimmed background)     │
│                                │
│    ┌──────────────────────┐    │
│    │                      │    │
│    │  ⚠ VERIFY PAYMENT    │    │
│    │                      │    │
│    │     ₹5,000           │    │
│    │                      │    │
│    │  This notification   │    │
│    │  looks unusual.      │    │
│    │  Please verify in    │    │
│    │  your UPI app before │    │
│    │  completing the      │    │
│    │  transaction.        │    │
│    │                      │    │
│    │  Reason: Notification│    │
│    │  from unknown source │    │
│    │                      │    │
│    │  ┌────────────────┐  │    │
│    │  │ Open PhonePe   │  │    │
│    │  └────────────────┘  │    │
│    │  ┌────────────────┐  │    │
│    │  │ Dismiss         │  │    │
│    │  └────────────────┘  │    │
│    │                      │    │
│    └──────────────────────┘    │
│                                │
└────────────────────────────────┘
```

**Warning popup design**:
- **Border**: 2dp solid `warn` (#E36161) around the popup card — visually distinct from normal green popup
- **Header**: "⚠ VERIFY PAYMENT" in `warn` red, 20sp bold — immediately alarming
- **Amount**: still shown, 40sp, white
- **Warning text**: 16sp, `text_on_bg`, explaining why it's suspicious
- **Reason tag**: 12sp, `text_secondary`, shows detection reason (e.g., "Unknown source app", "Notification structure mismatch", "Possible replay")
- **"Open [App]" button**: `primary` color, opens the actual UPI app so the merchant can verify
- **"Dismiss" button**: outlined, `text_secondary`
- **No auto-dismiss** — merchant must manually dismiss (unlike the 4-second auto-dismiss of normal popups)

**TTS warning announcement**:
- Instead of the normal announcement, speaks: "Warning! This payment may not be genuine. Please verify in your UPI app."
- Uses a distinctly different chime: 3 short warning beeps (ascending pitch)
- Spoken in a slightly slower pace for clarity

**Transaction log marking**:
- Suspicious transactions are flagged in the History screen with a warning icon
- Status: "Unverified ⚠" in `warn` color (instead of "Spoken ✓")
- Tapping shows the detection reason

**Settings additions**:
| Setting | Control type | Options | Default | Section |
|---------|-------------|---------|---------|---------|
| Fake payment detection | Toggle switch | Enable/disable | ON | SECURITY |
| Alert sensitivity | Dropdown | Low / Medium / High | Medium | SECURITY |

**New settings section** — SECURITY (added between DISPLAY and APP LANGUAGE):
```
│  SECURITY                      │
│  ┌────────────────────────┐    │
│  │ Fake payment        [⊙] │    │
│  │ detection               │    │
│  │ Warn about suspicious   │    │
│  │ notifications           │    │
│  ├────────────────────────┤    │
│  │ Alert sensitivity       │    │
│  │ Medium             [>]  │    │
│  └────────────────────────┘    │
```

**Sensitivity levels**:
| Level | Behavior |
|-------|----------|
| Low | Only alerts for notifications from completely unknown packages |
| Medium | Alerts for unknown packages + malformed notification structure |
| High | All of above + pattern mismatches + rapid duplicates + overlay detection |

**Edge cases**:
| Scenario | Behavior |
|----------|----------|
| Bank app not in known providers list | Show as "Unrecognized source" with option to whitelist |
| User whitelists a suspicious app | Saved in settings, not flagged again |
| New version of PhonePe changes notification format | Graceful degradation — announce normally but log a "format changed" event for debugging |

---

## 5. Component Library

### 5.1 Buttons

| Type | Background | Text color | Border | Usage |
|------|-----------|------------|--------|-------|
| Primary | `primary` (#5BD0AA) | `on_primary` (#0E1014) | none | Main actions (Start listening, Grant permission) |
| Secondary / Outlined | transparent | `primary` | 1dp `primary` | Secondary actions (Test, Open settings) |
| Text | transparent | `primary` | none | Tertiary actions (Skip, See all) |
| Danger | `warn` (#E36161) | white | none | Destructive actions (if any) |

All buttons: 52dp height, 12dp corner radius, 16sp semi-bold text, full width in most contexts.

### 5.2 Cards

- Background: `card_surface` (#1A1D24)
- Corner radius: 16dp
- Elevation: 2dp
- Padding: 16dp
- Margin between cards: 12dp

### 5.3 Toggle switches

- Track ON: `primary` (#5BD0AA)
- Track OFF: `divider` (#2A2D35)
- Thumb ON: white
- Thumb OFF: `text_secondary`

### 5.4 Bottom navigation bar

- Background: `card_surface` (#1A1D24)
- Height: 64dp
- Active item: `primary` icon + label
- Inactive item: `text_secondary` icon + label
- 3 items: Home, History, Settings
- Labels always visible (not hidden on inactive — better for non-tech-savvy users)
- Top border: 1dp `divider` color

### 5.5 Warning banner

- Background: `warn` at 12% opacity
- Left border: 4dp solid `warn`
- Icon: `warning` material icon, `warn` color
- Text: `text_on_bg`, 14sp
- Action text: `primary`, 14sp semi-bold, right-aligned
- Corner radius: 8dp
- Padding: 12dp

### 5.6 Chip (filter)

- Active: `primary` background, `on_primary` text
- Inactive: `card_surface` background, `text_secondary` text
- Height: 36dp
- Corner radius: 18dp (pill shape)
- Horizontal padding: 16dp
- Gap between chips: 8dp

### 5.7 Section header

- Text: all caps, `text_secondary`, 12sp, letter-spacing 1.5dp
- Margin top: 24dp
- Margin bottom: 8dp

### 5.8 List item (settings row)

- Height: minimum 56dp (grows with description text)
- Left: Label (16sp, `text_on_bg`) + optional description (12sp, `text_secondary`)
- Right: Control (toggle, chevron, or value text)
- Divider: 1dp `divider` color between items, indented 16dp from left

---

## 6. Interaction & Animation

### 6.1 Status indicator (Home screen)
- **Listening state**: Animated concentric sound waves pulsing outward from a speaker icon. Pulse interval: 2 seconds. Color: `primary` with decreasing opacity for each ring.
- **Paused state**: Static speaker icon with a slash through it. Grayscale.
- **Setup needed**: Static warning icon. Gentle pulsing glow in `warn` color.

### 6.2 Payment popup entrance
- Slide up from bottom + fade in (200ms, ease-out curve)
- Slight scale-up from 95% to 100%
- Background dim fades in simultaneously

### 6.3 Payment popup exit
- Fade out (150ms)
- Background dim fades out simultaneously

### 6.4 Auto-dismiss progress bar
- Thin horizontal bar at bottom of popup card
- Starts at full width, linearly shrinks to 0 over 4 seconds
- Color: `primary`
- Height: 3dp

### 6.5 Bottom navigation transitions
- Cross-fade between screens (200ms)
- No slide animations between tabs

### 6.6 Button press feedback
- Ripple effect on all clickable surfaces
- Primary buttons: darken to `primary_dark` on press
- Scale: subtle 0.98 scale on press (optional)

### 6.7 Toggle switch animation
- Smooth slide + color transition (150ms)

### 6.8 Transaction list
- New transactions animate in from top with a slide-down + fade-in

---

## 7. Responsive Layout

### Screen sizes
- **Target**: 5.0" – 6.7" phones (standard Indian market phones)
- **Minimum width**: 320dp
- **No tablet layout needed** for v1

### Orientation
- **Portrait only** — lock orientation. This app is a status dashboard, not a content app.

### Font scaling
- Support Android's font scaling up to 1.3x
- Test that all screens remain usable at largest font size
- Critical text (amounts, status) should not be clipped

### Safe areas
- Respect system bar insets (status bar, navigation bar, display cutouts)
- Bottom nav should sit above the gesture navigation bar

---

## 8. Accessibility

- **Minimum touch target**: 48dp × 48dp for all interactive elements
- **Color contrast**: All text meets WCAG AA (4.5:1 for body text, 3:1 for large text) — verify against dark background
- **Content descriptions**: All icons must have `contentDescription` for TalkBack
- **No critical information conveyed by color alone** — always pair color with text/icon
- **Focus order**: Logical top-to-bottom, left-to-right reading order
- **Large text support**: Key information (amounts, status) in 18sp+
- **No tiny text**: Minimum 12sp for any visible text

---

## 9. Localization

### Currently supported
| Language | Tag | UI text | TTS |
|----------|-----|---------|-----|
| English (India) | `en-IN` | Yes | Yes |
| Kannada | `kn-IN` | Yes | Yes |

### Planned (Milestone 4+)
| Language | Tag | UI text | TTS |
|----------|-----|---------|-----|
| Hindi | `hi-IN` | Planned | Planned |
| Marathi | `mr-IN` | Planned | Planned |
| Tamil | `ta-IN` | Planned | Planned |
| Telugu | `te-IN` | Planned | Planned |
| Bengali | `bn-IN` | Planned | Planned |

### Localization guidelines
- All user-visible strings must be in `strings.xml` (no hardcoded text)
- Amounts always use ₹ symbol (not "Rs." or "INR")
- Number formatting: Indian system (12,50,000 not 1,250,000)
- Date formatting: Indian standard (20 May 2026, not May 20, 2026)
- Time formatting: 12-hour with AM/PM (2:34 PM)

---

## 10. Edge Cases & Empty States

| State | Design |
|-------|--------|
| No transactions yet | Illustration + "No transactions yet — make a small test payment to verify." |
| Permissions not granted | Warning banner + "Setup needed" status + "Fix" button |
| TTS engine not installed | Settings shows banner: "Google TTS not installed" + "Install" button |
| No internet (irrelevant) | App works fully offline — no special state needed |
| Multiple popups queued | Show one at a time, queue the rest. Each gets 4 seconds. |
| Very large amount (₹99,99,999) | Amount text should scale down if needed (auto-size) |
| Dual TTS with same language selected | Show a hint: "Choose a different second language" |
| Provider not yet implemented | Settings toggle disabled + "Coming soon" label |
| No payments for daily summary | Summary notification: "No payments received today" — still useful confirmation |
| Shake triggered accidentally | Low sensitivity by default; disable shake during phone calls |
| Fake detection false positive | "Dismiss" button + option to whitelist the source app |
| New UPI app version changes format | Announce normally, log "format changed" for debugging |
| Chime + DND mode | Respect DND unless "override DND" is enabled |
| Summary time during active TTS | Queue summary after current announcement finishes |

---

## 11. File Structure for Design Assets

```
docs/
 └─ DESIGN.md              ← This file
 └─ design/
     ├─ screens/
     │   ├─ onboarding-welcome.png
     │   ├─ onboarding-permission.png
     │   ├─ onboarding-test.png
     │   ├─ home-listening.png
     │   ├─ home-listening-with-fab.png
     │   ├─ home-setup-needed.png
     │   ├─ home-with-transactions.png
     │   ├─ history-full.png
     │   ├─ history-empty.png
     │   ├─ history-daily-summaries.png
     │   ├─ settings.png
     │   ├─ settings-chime-picker.png
     │   ├─ payment-popup.png
     │   ├─ payment-popup-kannada.png
     │   ├─ payment-popup-warning.png
     │   ├─ daily-summary-detail.png
     │   └─ daily-summary-share.png
     ├─ components/
     │   ├─ buttons.png
     │   ├─ cards.png
     │   ├─ bottom-nav.png
     │   ├─ fab-replay.png
     │   ├─ warning-banner.png
     │   ├─ fake-payment-alert.png
     │   └─ toggle-switches.png
     └─ tokens/
         ├─ colors.png
         └─ typography.png
```

---

## 12. Implementation Notes

### Current tech stack
- **UI framework**: ViewBinding (XML layouts) — Compose migration planned for Milestone 3+
- **Theme**: Material Components (M2) DayNight with forced dark
- **Navigation**: Currently Activity-based; consider migrating to Jetpack Navigation with BottomNavigationView for the 3-tab layout
- **State management**: SharedPreferences (Settings.kt) — DataStore migration planned

### What needs to be built

#### Core screens
- [ ] Settings screen layout + Activity/Fragment
- [ ] Transaction History screen layout + Activity/Fragment  
- [ ] BottomNavigationView integration (replace current button-based navigation)
- [ ] Status animation on Home screen (sound-wave pulse)
- [ ] Payment popup: add progress bar, slide-up animation, source app label
- [ ] Localized strings for Kannada UI (`values-kn/strings.xml`)
- [ ] Filter chips on History screen
- [ ] Date group headers in transaction list

#### Repeat Last Announcement
- [ ] FAB on Home screen (replay icon, accent color)
- [ ] Shake-to-repeat gesture detection (accelerometer in foreground service)
- [ ] "Repeat" action button on persistent foreground notification
- [ ] Last-announcement cache in memory (amount, language, utterance text)
- [ ] Shake sensitivity setting (Off / Low / Medium / High)

#### Distinct Chime Before TTS
- [ ] Bundle chime audio files (`res/raw/chime_classic.ogg`, `chime_cashregister.ogg`, `chime_digital.ogg`, `chime_coin.ogg`)
- [ ] SoundPool or MediaPlayer integration — play chime before TTS on STREAM_ALARM
- [ ] Chime picker bottom sheet UI with preview playback
- [ ] Amount-based chime intensity logic (single/double/triple)
- [ ] Custom chime import from device storage
- [ ] Settings: chime selection, large amount chime toggle

#### Daily Summary
- [ ] Daily summary data aggregation (total amount, count, by-provider breakdown, min/max/average)
- [ ] Summary notification at configurable time (AlarmManager scheduled)
- [ ] Summary detail screen (hero card + provider breakdown + stats)
- [ ] TTS summary announcement (optional, spoken at summary time)
- [ ] Share summary as text via Android share sheet
- [ ] Historical summaries list (accessible from History tab)
- [ ] Settings: summary toggle, time picker, speak-summary toggle

#### Fake Payment Detection
- [ ] Package verification layer (check notification source package against known genuine packages)
- [ ] Notification structure validation (verify extras format matches real UPI apps)
- [ ] Pattern mismatch detection (text analysis for suspicious notification text)
- [ ] Rapid duplicate detection (same amount from same source within seconds)
- [ ] Warning popup UI (red border, "VERIFY PAYMENT" header, reason tag, "Open App" button)
- [ ] Warning TTS announcement (different chime + slower "Warning! Verify payment" speech)
- [ ] Transaction log flagging (unverified status with warning icon)
- [ ] App whitelist management (user can approve unrecognized sources)
- [ ] Settings: detection toggle, sensitivity level (Low / Medium / High)
- [ ] SECURITY section in Settings screen

### What already exists
- [x] Home screen (basic layout with status + buttons + empty transaction area)
- [x] Onboarding wizard (step-by-step with permission granting)
- [x] Payment popup (basic — amount + words, auto-dismiss)
- [x] Dark theme colors and Material Components theme
- [x] All backend logic (TTS, parsing, services, settings storage)
- [x] PhonePe notification parser (Milestone 2)
- [x] Kannada TTS number-to-words (Milestone 2)
- [x] Dual-TTS mode (Milestone 2)
- [x] Alarm stream volume maximization (Milestone 2)
