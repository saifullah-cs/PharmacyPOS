# Pharmacy POS - Point of Sale Retail System

A desktop-based Point of Sale (POS) application built for pharmacy retail management. 
Handles inventory, billing, invoicing, distributor management, and sales reporting — 
designed to be a real, deployable system for small pharmacy stores.

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

- **Language:** Java 26
- **Database:** MySQL
- **Database Access:** JDBC
- **IDE:** NetBeans
- **Packaging:** Executable JAR

## Prerequisites

Before running this application, make sure you have:

- Java 26 (or compatible JDK) installed
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
- Run the SQL files located in the `DataBase/Pharmacy_POS.SQL/` folder to create the required tables:
  - `pharmacy_pos_medicines.sql`
  - `pharmacy_pos_sales.sql`
  - `pharmacy_pos_users.sql`
  - `pharmacy_pos_distributors.sql`
  - `pharmacy_pos_app_settings.sql`
  - `pharmacy_pos_stock_adjustments.sql`
  - `pharmacy_pos_tests.sql`
  - `pharmacy_pos_sale_tests.sql`

### 3. Configure the database connection
- Locate the database configuration in `DBConnection.java`
- Update the connection URL, username, and password to match your local MySQL setup

### 4. Run the application
Download the latest `.jar` file from the [Releases](../../releases) page, then run:
```bash
java -jar PharmacyPOS.jar
```

Or build from source using NetBeans (Clean and Build), then run the generated JAR from the `dist` folder.

## Project Structure