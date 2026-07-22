# Pharmacy POS - Point of Sale Retail System

A desktop-based Point of Sale (POS) application built for pharmacy retail management. 
Handles inventory, billing, invoicing, distributor management, and sales reporting.

## Features

- **Medicine Inventory Management** — add, update, and track medicine stock
- **Sales / Billing** — process transactions with real-time bill generation
- **Invoice Generation & Printing** — printable invoices for every sale
- **User Authentication** — secure login system
- **Distributor Management** — track and manage medicine distributors/suppliers
- **Low Stock Alerts** — configurable stock threshold warnings
- **Sales Reports & History** — view past sales and generate reports
- **Test Management** — manage lab test records and fees

## Tech Stack

- **Language:** Java
- **Database:** MySQL
- **Database Access:** JDBC
- **IDE:** NetBeans
- **Packaging:** Executable JAR

## Prerequisites

- Java (JDK) installed
- MySQL Server installed and running
- A MySQL client (MySQL Workbench, or command line) to set up the database

## Setup Instructions

### 1. Clone the repository
```bash
git clone https://github.com/saifullah-cs/PharmacyPOS.git
cd PharmacyPOS
```

### 2. Set up the database
- Open MySQL Workbench (or your preferred MySQL client)
- Create a new database (e.g., `pharmacy_pos`)
- Run the SQL files located in the `DataBase/Pharmacy_POS.SQL/` folder to create the required tables

### 3. Configure the database connection
- Copy `config.properties.example` and rename the copy to `config.properties`
- Open `config.properties` and fill in your actual database details:
```properties
db.url=jdbc:mysql://localhost:3306/pharmacy_pos
db.user=root
db.password=your_password_here
```
- This file is git-ignored, so your credentials stay local and are never pushed to GitHub

### 4. Run the application
Download the latest `.jar` file from the [Releases](../../releases) page, then run:
```bash
java -jar PharmacyPOS.jar
```

Or build from source using NetBeans (Clean and Build), then run the generated JAR from the `dist` folder.

## Project Structure