# Locales

Per-locale conventions, taken from the existing translations. Folder names are the `values-` suffix.

- **Address**: how the app speaks to the listener. Use it in every string that addresses them.
- **Plurals**: the quantities every `<plurals>` entry in that locale needs.
- **Known mistakes**: existing strings that break the convention. Fix them when you work on that locale.

The four groups are the split for parallel subagents.

## Group A: Germanic, Nordic, Finnic

| Folder | Language         | Address                        | Quotes | Plurals    | Notes                                                       |
|--------|------------------|--------------------------------|--------|------------|-------------------------------------------------------------|
| de     | German           | Du, capitalized: Du, Dein, Dir | „…“    | one, other | Actions in the infinitive: Notiz hinzufügen, Feedback geben |
| et     | Estonian         | sina: sa, oma                  | „…“    | one, other |                                                             |
| fi     | Finnish          | sinä: et, kirjastostasi        | ”…”    | one, other |                                                             |
| nb-rNO | Norwegian Bokmål | du                             | «…»    | one, other | BCP 47 `nb-NO`                                              |
| nl-rNL | Dutch            | je, jouw; never u              | “…”    | one, other | BCP 47 `nl-NL`                                              |
| sv     | Swedish          | du                             | ”…”    | one, other |                                                             |

## Group B: Romance

| Folder | Language              | Address                                      | Quotes               | Plurals          | Notes                                                                                                                                                                         |
|--------|-----------------------|----------------------------------------------|----------------------|------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| es     | Spanish               | tú                                           | «…»                  | one, many, other | Neutral Spanish for Spain and Latin America. Actions in the infinitive: Añadir nota                                                                                           |
| fr     | French                | vous                                         | « … », spaces inside | one, many, other |                                                                                                                                                                               |
| it     | Italian               | tu                                           | «…»                  | one, many, other |                                                                                                                                                                               |
| pt-rBR | Portuguese (Brazil)   | você                                         | “…”                  | one, many, other | Brazilian words: arquivo, celular. BCP 47 `pt-BR`. Known mistakes: European Portuguese (ficheiro, Isto apagará) in `book.delete.*`, `book.edit.cover.file`, `folder.type.file_count` |
| pt-rPT | Portuguese (Portugal) | formal, without a pronoun: Agite o telemóvel | «…»                  | one, many, other | European words: ficheiro, telemóvel. BCP 47 `pt-PT`                                                                                                                            |
| ro     | Romanian              | tu: Alege, ta                                | „…”                  | one, few, other  |                                                                                                                                                                               |

## Group C: Slavic, Hungarian

| Folder | Language  | Address                    | Quotes | Plurals               | Notes                                                                                           |
|--------|-----------|----------------------------|--------|-----------------------|-------------------------------------------------------------------------------------------------|
| cs     | Czech     | vy, lowercase              | „…“    | one, few, many, other |                                                                                                 |
| hu     | Hungarian | Ön: Válassza, könyvtárában | „…”    | one, other            | Actions as nouns: Hozzáadás, Mappa kiválasztása. Suffixes on Voice take a hyphen: Voice-szal |
| pl     | Polish    | ty: Potrząśnij, swoją      | „…”    | one, few, many, other |                                                                                                 |
| ru     | Russian   | вы, lowercase              | «…»    | one, few, many, other |                                                                                                 |
| sk     | Slovak    | vy, lowercase              | „…“    | one, few, many, other |                                                                                                 |
| uk     | Ukrainian | ви, lowercase              | «…»    | one, few, many, other |                                                                                                 |

## Group D: Middle East, Asia, Turkish

| Folder | Language             | Address                                                                                 | Quotes      | Plurals         | Notes                                                                                                                                                   |
|--------|----------------------|-----------------------------------------------------------------------------------------|-------------|-----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| fa-rIR | Persian              | شما: دستتان                                                                            | «…»         | one, other      | BCP 47 `fa-IR`                                                                                                                                          |
| in     | Indonesian           | Anda                                                                                    | “…”         | other           | BCP 47 `id`                                                                                                                                             |
| iw     | Hebrew               | gender-neutral: plural (בחרו, שלכם) or impersonal (ניתן, אפשר)                         | `\"…\"`     | one, two, other | Actions as nouns: הוספת הערה, חיפוש. BCP 47 `he`. Known mistakes: masculine singular imperatives (הוסף, נער, הצע, דווח, תהנה)                              |
| ja     | Japanese             | polite です/ます, no あなた                                                             | 「…」       | other           | Buttons in plain form or as nouns: フィードバックを送る, メモを追加. Full-width ！ and ？. A space around Latin words: Google Play で評価                    |
| ta     | Tamil                | நீங்கள்: உங்கள்                                                                          | “…”         | one, other      |                                                                                                                                                         |
| tr     | Turkish              | siz in sentences (bulabilirsiniz, kitaplarınız), bare imperative on buttons (Ekle, Seç) | “…”         | one, other      | Suffixes on Voice take an apostrophe: `Voice\'u`. Known mistakes: sen in sentences (klasörü seç, Aklında ne var?, dinlediğin)                             |
| zh-rCN | Chinese (Simplified) | 您                                                                                      | “…”, 《…》 for book titles | other | Full-width ，。！？. A space between Chinese and Latin words or numbers: 使用 Voice 收听. BCP 47 `zh-CN`                                                    |
