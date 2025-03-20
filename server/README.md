# Mujde Project - Server Side

## Overview
The server-side component of the Mujde project is a RESTful Java application that wraps an SQLite database and provides an HTTP API for managing Frida scripts and their association with Android applications. It enables users to store, retrieve, and update script configurations remotely.

## Features
- **RESTful API**: Built using JAX-RS to handle HTTP requests.
- **SQLite Database**: Stores information about scripts and their associations with Android applications.
- **Script Management**:
  - Store and retrieve Frida scripts.
  - Update scripts sourced from the internet.
  - Associate scripts with installed applications.
- **Backup and Restore**:
  - the server stores the scripts uploaded by the clients, in a unique folder at "./stored_scripts/" from server's cwd. if the folder didn't exist previously, the server creates it.

## API Endpoints
### Application Management
NOTE that app-id is the package name, e.g. `com.tranzmate`
- `GET /apps` - Retrieve all registered applications.
- `POST /apps` - Register a new application.
- `GET /apps/{id}` - Retrieve details of a specific application.
- `DELETE /apps/{app_id}` - Remove an application from DB, including all injections to it.

### Script Management
- `GET /scripts` - Retrieve all available scripts.
- `POST /scripts` - Upload a new script, include it's content. If the script does not exist, the server selects a new random-path over the filesystem
- `GET /scripts/{script_id}` - Retrieve a specific script.
- `PUT /scripts/{script_id}` - Update content of an existing script.
- `DELETE /scripts/{script_id}` - Remove a script.

### Injection Management
- `GET /injections/by_app/{package_name}` - Get list of scripts to inject into selected app.
- `GET /injections/by_script/{script_name}` - Get list of apps to inject into .
- `POST /injections` - Associate a script with an application.
- `DELETE /injections/{package_name}/{script_name}` - Remove an association.

## Database Schema
### `Apps` Table
| Column       | Type   | Description |
|-------------|--------|-------------|
| `app_id`    | INT (PK) | Unique identifier for an application. |
| `package_name` | TEXT | Package name of the application. |

### `Scripts` Table
| Column       | Type   | Description |
|-------------|--------|-------------|
| `script_id` | INT (PK) | Unique script identifier. |
| `script_name` | TEXT | Short name that describes the meaning of the script. this field is non-empty and unique. |
| `script_path` | TEXT | Local file system path (path where the script content is stored on server's filesystem). |
| `network_path` | TEXT | URL of the script (optional. http url to script-file). |
| `last_modified` | DATETIME | Timestamp where the script content was updated in the last time. |

### `Injections` Table
| Column    | Type   | Description |
|-----------|--------|-------------|
| `app_id`  | INT (PK, FK) | Associated application. |
| `script_id` | INT (PK, FK) | Associated script. |
