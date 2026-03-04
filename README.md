# 🛰️ SpaceKeeperFX

<p align="center">
  <img src="src/main/resources/icons/app.png" alt="SpaceKeeperFX Logo" width="120" />
</p>

<p align="center">
  <b>Local-first vault and record manager built with JavaFX + SQLite.</b><br/>
  Organize data into Vaults and Subspaces, customize columns, and attach photos per record.
</p>

<p align="center">
  <img alt="Java" src="https://img.shields.io/badge/Java-22-ED8B00?logo=openjdk&logoColor=white">
  <img alt="UI" src="https://img.shields.io/badge/UI-JavaFX%2024-1E88E5">
  <img alt="DB" src="https://img.shields.io/badge/Database-SQLite-003B57?logo=sqlite">
  <img alt="Build" src="https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven&logoColor=white">
  <img alt="Mode" src="https://img.shields.io/badge/Mode-Offline%20Local--First-2E7D32">
</p>

<p align="center">
  <a href="#-quick-start">Quick Start</a> •
  <a href="#-core-capabilities">Core Capabilities</a> •
  <a href="#-architecture">Architecture</a> •
  <a href="#-data--storage">Data & Storage</a> •
  <a href="#-roadmap">Roadmap</a>
</p>

---

## 📌 What Is SpaceKeeperFX?

**SpaceKeeperFX** is a desktop app for structured local data management.

It uses a **Vault (.db file)** model where each vault contains multiple **Subspaces** (tables), and each subspace supports configurable columns:
- `TEXT`
- `NUMBER`
- `PHOTO`

The app is designed for local workflows where you want:
- clear table-style organization,
- editable schemas,
- image attachments per row,
- and zero cloud dependency.

---

## ✨ Core Capabilities

### 🗄️ Vault Workflow
- Create a new vault (`.db`) from the UI.
- Open existing vault files from disk.
- Auto-bootstrap schema via migration runner on first open.
- Close and switch vaults without restarting the app.

### 🧭 Subspace Management
- Create subspaces per vault.
- Optional default photograph column at subspace creation.
- Rename and delete subspaces from sidebar context menu.
- Fast switching between subspaces.

### 🧱 Dynamic Columns
- Baseline columns: `Name / ID`, `Photograph`, `Description`.
- Add new `TEXT`, `NUMBER`, and `PHOTO` columns.
- Reorder columns (up/down), rename by double-click, enable/disable.
- Persist schema edits into SQLite (`column_def`).

### 🧾 Record Editing
- Add blank rows and delete multi-selected rows.
- Inline editing for text cells.
- Number editing via context action.
- Per-cell persistence using upsert semantics in `record_value`.

### 🖼️ Photo Attachment
- Import image files directly from table cells.
- Supported formats: `png`, `jpg`, `jpeg`, `gif`, `bmp`, `webp`.
- Photos are copied into managed local storage and referenced by path.
- Per-vault photo root keeps media scoped to each vault database.

### 🎛️ Desktop UX
- Custom undecorated window with custom title bar controls.
- Manual edge/corner resize handling.
- Collapsible sidebar split layout.
- Light/Dark theme toggle.

---

## 🧱 Architecture

```text
JavaFX UI (FXML + Controllers)
   -> Services (Image handling, window behavior)
   -> Repositories (Vault/Subspace/Record abstractions)
   -> DAOs (SQL operations)
   -> SQLite (vault, subspace, column_def, record, record_value)
```

### Layer Breakdown
- `ui`: JavaFX controllers, dialogs, custom table cells.
- `service`: photo file handling and window resize behavior.
- `repository`: app-facing operations over DAO layer.
- `persistence.dao`: SQL writes/reads.
- `persistence`: DB bootstrap and migration orchestration.
- `model`: domain objects (`Vault`, `Subspace`, `Record`, `ColumnDef`).

---

## 🚀 Quick Start

### 1. Prerequisites
- JDK `22`
- Maven `3.9+`

### 2. Clone and run

```bash
git clone https://github.com/MaheshChandraTeja/SpaceKeeper.git
cd SpaceKeeper
mvn javafx:run
```

### 3. Alternate run script (Unix-like shells)

```bash
bash scripts/run-dev.sh
```

> If JavaFX rendering has GPU issues on your machine, try:
>
> `mvn -Dprism.order=sw javafx:run`

---

## 🛠️ Build

```bash
mvn -DskipTests package
```

Build artifact is created in `target/`.

---

## 🗃️ Data & Storage

By default, app data lives under:

```text
~/.spacekeeper/
├─ spacekeeper.db
├─ vaults/
└─ photos/
```

When opening a specific vault database, photos are scoped per vault file:
- `<vault_name>_photos/`

### SQLite Schema Highlights
- `vault`: vault metadata
- `subspace`: logical table groups per vault
- `column_def`: configurable column schema per subspace
- `record`: row identity and timestamps
- `record_value`: EAV cell storage (`text_value` / `number_value`)

Migration strategy uses `PRAGMA user_version` and versioned SQL plan execution.

---

## 📂 Project Structure

```text
src/main/java/com/spacekeeperfx
├─ App.java
├─ config/
├─ model/
├─ persistence/
│  ├─ dao/
│  └─ mapper/
├─ repository/
├─ service/
└─ ui/
   └─ components/

src/main/resources
├─ fxml/
├─ css/
├─ db/
└─ icons/
```

---

## 🧭 Roadmap

- [ ] Validation rules in UI for required/unique columns
- [ ] Search/filter/sort controls per subspace table
- [ ] CSV import/export flows
- [ ] Better packaging pipeline (`jlink` / `jpackage`) alignment
- [ ] Automated test coverage for DAO + repository layers

---

## 🤝 Contributing

Suggestions, bug reports, and PRs are welcome.

If you open an issue, include:
- OS and Java version
- steps to reproduce
- logs or stack trace (if available)

---

## 👤 Author

Built by [MaheshChandraTeja](https://github.com/MaheshChandraTeja)

