# Locales

Per-locale conventions, taken from the existing translations. Folder names are the `values-` suffix.

- **Address**: how the app speaks to the listener. Use it in every string that addresses them.
- **Plurals**: the quantities every `<plurals>` entry in that locale needs.
- **Known mistakes**: existing strings that break the convention. Fix them when you work on that locale.

The four groups are the split for parallel subagents.

## Group A: Germanic, Nordic, Finnic

| Folder | Language         | Address                        | Quotes | Plurals    | Notes                                                       |
|--------|------------------|--------------------------------|--------|------------|-------------------------------------------------------------|
| da     | Danish           | du                             | »…«    | one, other |                                                             |
| de     | German           | Du, capitalized: Du, Dein, Dir | „…“    | one, other | Actions in the infinitive: Notiz hinzufügen, Feedback geben |
| et     | Estonian         | sina: sa, oma                  | „…“    | one, other |                                                             |
| fi     | Finnish          | sinä: et, kirjastostasi        | ”…”    | one, other |                                                             |
| nb-rNO | Norwegian Bokmål | du                             | «…»    | one, other | BCP 47 `nb-NO`                                              |
| nl-rNL | Dutch            | je, jouw; never u              | “…”    | one, other | BCP 47 `nl-NL`                                              |
| sv     | Swedish          | du                             | ”…”    | one, other |                                                             |

## Group B: Romance

| Folder | Language              | Address                                      | Quotes               | Plurals          | Notes                                                                                                                                                                         |
|--------|-----------------------|----------------------------------------------|----------------------|------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ca     | Catalan               | vós in sentences: Trieu, vostra; singular imperative on buttons: Afegeix, Desa | «…»                  | one, many, other | Progress as S'està baixant…, never a gerund. Voice without an article: amb Voice                                                                                              |
| es     | Spanish               | tú                                           | «…»                  | one, many, other | Neutral Spanish for Spain and Latin America. Actions in the infinitive: Añadir nota                                                                                           |
| fr     | French                | vous                                         | « … », spaces inside | one, many, other |                                                                                                                                                                               |
| it     | Italian               | tu                                           | «…»                  | one, many, other |                                                                                                                                                                               |
| pt-rBR | Portuguese (Brazil)   | você                                         | “…”                  | one, many, other | Brazilian words: arquivo, celular. BCP 47 `pt-BR` |
| pt-rPT | Portuguese (Portugal) | formal, without a pronoun: Agite o telemóvel | «…»                  | one, many, other | European words: ficheiro, telemóvel. BCP 47 `pt-PT`                                                                                                                            |
| ro     | Romanian              | tu: Alege, ta                                | „…”                  | one, few, other  |                                                                                                                                                                               |
| sc     | Sardinian             | tu: ses, podes, tuo                          | “…”                  | one, other       | Logudorese-leaning LSC: sos/sas, lu, dae, never Campidanese is, ddu. Prosthetic i: ischermu, isvilupu. See [Sardinian](#sardinian)                                             |

## Group C: Slavic, Greek, Hungarian

| Folder | Language  | Address                    | Quotes | Plurals               | Notes                                                                                           |
|--------|-----------|----------------------------|--------|-----------------------|-------------------------------------------------------------------------------------------------|
| cs     | Czech     | vy, lowercase              | „…“    | one, few, many, other |                                                                                                 |
| el     | Greek     | εσείς: Επιλέξτε, σας       | «…»    | one, other            | Question mark is `;`. Actions as nouns: Προσθήκη σημείωσης                                      |
| hu     | Hungarian | Ön: Válassza, könyvtárában | „…”    | one, other            | Actions as nouns: Hozzáadás, Mappa kiválasztása. Suffixes on Voice take a hyphen: Voice-szal |
| mk     | Macedonian | вие, lowercase, in sentences: Изберете, вашите; bare imperative on buttons: Додај | „…“    | one, other            | фолдер, and аудио книга as two words                                                            |
| pl     | Polish    | ty: Potrząśnij, swoją      | „…”    | one, few, many, other |                                                                                                 |
| ru     | Russian   | вы, lowercase              | «…»    | one, few, many, other |                                                                                                 |
| sk     | Slovak    | vy, lowercase              | „…“    | one, few, many, other |                                                                                                 |
| uk     | Ukrainian | ви, lowercase              | «…»    | one, few, many, other |                                                                                                 |

## Group D: Middle East, Asia, Turkish

| Folder | Language             | Address                                                                                 | Quotes      | Plurals         | Notes                                                                                                                                                   |
|--------|----------------------|-----------------------------------------------------------------------------------------|-------------|-----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| ar     | Arabic               | gender-neutral: verbal nouns on buttons (إضافة, حفظ), sentences with يمكنك + verbal noun or impersonal; no masculine imperative (أضف) or adjective about the listener (أنت جاهز) | «…»         | zero, one, two, few, many, other | Voice in Latin script, never فويس. A string that starts with a Latin word flips the line to left-to-right, so start with Arabic                         |
| fa-rIR | Persian              | شما: دستتان                                                                            | «…»         | one, other      | BCP 47 `fa-IR`                                                                                                                                          |
| hi     | Hindi                | आप: चुनें, हटाएँ, आपकी                                                                  | “…”         | one, other      | Verbs with आप show gender (सकते/सकती): use dative, passive or nouns instead. Book is पुस्तक. फ़ with a nukta: फ़ाइल, फ़ोल्डर, फ़ोन                                 |
| in     | Indonesian           | Anda                                                                                    | “…”         | other           | BCP 47 `id`                                                                                                                                             |
| iw     | Hebrew               | gender-neutral: plural (בחרו, שלכם) or impersonal (ניתן, אפשר)                         | `\"…\"`     | one, two, other | Actions as nouns: הוספת הערה, חיפוש. BCP 47 `he`                                                                                                        |
| ja     | Japanese             | polite です/ます, no あなた                                                             | 「…」       | other           | Buttons in plain form or as nouns: フィードバックを送る, メモを追加. Full-width ！ and ？. A space around Latin words: Google Play で評価                    |
| ta     | Tamil                | நீங்கள்: உங்கள்                                                                          | “…”         | one, other      |                                                                                                                                                         |
| tr     | Turkish              | siz in sentences (bulabilirsiniz, kitaplarınız), bare imperative on buttons (Ekle, Seç) | “…”         | one, other      | Suffixes on Voice take an apostrophe: `Voice\'u`                                                                                                          |
| zh-rCN | Chinese (Simplified) | 您                                                                                      | “…”, 《…》 for book titles | other | Full-width ，。！？. A space between Chinese and Latin words or numbers: 使用 Voice 收听. BCP 47 `zh-CN`                                                    |

## Sardinian

Models know little Sardinian and fill the gaps with Italian words. A native speaker corrected these in #4014:

- perunu, not nissunu. cada, not onni. como, not immoe. petzi, not isceti. in antis, not prima.
  deretu a pustis, not luego a pustis.
- Sardinian words, not Italian ones: balangiare, not guadangiare. mùtzina, not retàllia. parastàgiu, not iscaffale.
  padente, not foresta. elaboradore, not computer. te, not tè.
- tzicherone for a cup of tea, tzìchera for a coffee cup. iscutzinare for shaking the phone, iscùdere means to hit.
- issu and issa only for people. For a thing, use cussu and cussa: “Chene cussu, Voice non podet…”.
- Mark the accent on words stressed on the third-to-last syllable: càmbia, òrdina, iscùtzina, cùfias, interrùmpidu.

For a word you're unsure of, look up its Italian in Apertium's
[Sardinian–Italian dictionary](https://raw.githubusercontent.com/apertium/apertium-srd-ita/master/apertium-srd-ita.srd-ita.dix):
`grep '<r>tazza<s'` finds the Sardinian in `<l>`. Skip entries marked `r="LR"`, those forms aren't preferred. For a UI
term, search the native speaker's
[translation memory of open source apps](https://github.com/user-attachments/files/33915708/Sardu-memoria.intrea.tmx.zip):
`grep -i -A6 '^ *Bookmark added *$'` prints the Sardinian below the English. It mixes in Campidanese and has mistakes,
so it doesn't override this list. Spelling, accents, pronouns and adverbs follow the
[official LSC rules](https://www.regione.sardegna.it/documenti/1_72_20060418160308.pdf).
