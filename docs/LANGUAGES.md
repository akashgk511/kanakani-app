# Languages

The app shows **one language at a time**. People pick English, বাংলা or हिन्दी from the language control in the top bar, and the choice is remembered. They can change it whenever they like.

## Prototype (`prototype/kanakani.html`)
- A language menu sits in the top bar and on the sign-up screen. The choice is saved on the device.
- Every screen is drawn in English and translated as it appears, so nothing flashes in the wrong language. Text without a translation stays in English.
- **Not translated on purpose:** secret names (they are identities), company names, and the made-up sample posts and chats (they stand in for what people write).
- Bengali and Hindi turn off the wide letter-spacing used for English labels, because it pulls joined letters apart. That was the cause of the broken "কানাকানি" under the logo.
- Fonts: Noto Sans Bengali and Noto Sans Devanagari are added next to the existing fonts.

## Java backend
- `LangFilter` reads `?lang=en|bn|hi`, saves it in the `kn_lang` cookie and uses it on later pages. Only those three codes are accepted.
- Pages read their text from `i18n/messages*.properties` with `<fmt:message key="..."/>`. A key missing in Bengali or Hindi falls back to English.
- **Done:** top bar, tabs, sign-in page, error page.
- **Still to do:** Feed, Ranks, Jobs, Wishbox and Security pages, plus the messages the Java code sends back (notices and errors). Move each piece of text into the three files; `SelfTest` checks that all three files have the same keys.
- Add a language by creating `messages_xx.properties`, adding its code to `LangFilter`, and adding a link in `_header.jsp`.

## Quality note
The Bengali and Hindi text was written by an AI model for a playful tone. Have a native speaker of each language read it before launch, especially the jokes and the safety wording.
