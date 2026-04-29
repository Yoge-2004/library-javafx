# LMSJavaFX

📚 A desktop Library Management System (LMS) built with JavaFX.  
✅ Manage books, users, and issue/return workflows in a clean GUI.

## ✨ Features

- 🔐 Staff login with validation and feedback
- 📚 Book management: add, update, delete, search, availability tracking
- 👤 User management: register users, view details, manage profiles
- 🔁 Issue/return workflow with due-date tracking
- 🔎 Search and filtering across lists
- 💾 OS-aware local storage for data, exports, backups, config, and logs

## 🖼️ Screenshots

**Login Screen**  
Simple staff login with validation.

![Login Screen](assets/screenshots/login.png)

**Main Dashboard**  
Central workspace with quick actions and overview.

![Dashboard](assets/screenshots/dashboard.png)

**Issued Books View**  
Track issued items and manage returns.

![Issued Books](assets/screenshots/dashboard_books_issued.png)

## ⚙️ Requirements

- JDK 26
- Internet access on first build (downloads dependencies)

## 🚀 Quick Start (IDE Run Button)

Import as a Maven project, then run:

- the shared `LibraryApp` run configuration from the IDE, or
- the Maven goal `javafx:run`

The shared run configuration includes `--enable-native-access=javafx.graphics`, so the JavaFX 26 native-loader warning is suppressed for direct IDE runs. If you prefer Maven:

```powershell
.\mvnw -q javafx:run
```

## 🧱 Build

Create the application jar:

```powershell
.\mvnw -q -DskipTests package
```

## 🗂️ Data Files

The app no longer depends on a repo-local `data/` folder at runtime. By default it stores files under OS-specific application directories:

- Windows: `%APPDATA%\LibraryOS` and `%LOCALAPPDATA%\LibraryOS\logs`
- macOS: `~/Library/Application Support/LibraryOS` and `~/Library/Logs/LibraryOS`
- Linux: `${XDG_DATA_HOME:-~/.local/share}/LibraryOS` and `${XDG_STATE_HOME:-~/.local/state}/LibraryOS/logs`

Data and export locations can still be overridden from the app configuration screen.

## 🧭 Project Layout

```
LMSJavaFX/
├── src/
│   └── main/
│       └── java/
│           └── com/example/...
├── assets/
│   └── screenshots/
├── pom.xml
└── README.md
```

## 🛠️ Troubleshooting

- **JavaFX runtime error on `java -jar`:**  
  Use the IDE run configuration or `javafx:run`. A plain jar does not bundle the JavaFX runtime.

- **Maven not found:**  
  Use the Maven Wrapper included in this repo:
  ```powershell
  .\mvnw -v
  ```

## 📄 License

Apache License 2.0. See `LICENSE`.

## 👤 Author

Yogeshwaran
