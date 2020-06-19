# Team Rocket's Bird Tracking app

This repository holds the source code for the *Bird Tracking* app developed as part of the course project for Introduction to Software Engineering for Engineers course at OVGU.

## Basic Prototype

Currently, all the essential features have been implemented.

### Essential features

- [x] User can add a bird sighting with name, category, photo, location, and date and time.
- [x] For a sighting, show all similar appearances as markers on the map.
- [x] User can add their own categories when adding a bird sighting.
- [x] While adding a bird sighting, show a link to more information from Wikipedia.

## Advanced Prototype

### Essential

- [x] User can filter sightings by date, category, and location.
- [x] Add description field to show bird information on the map screen.
- [x] Add notes field while adding a bird sighting.
- [x] Add multiple language translations for the app.

### Necessary
- [x] Link to more information in the app itself.

More implemented features of the app are listed in the [User Stories](../wikis/User-Stories) wiki page.

### [Download APK file](https://code.ovgu.de/steup/rocket/-/raw/dev/app/release/app-release-vM3.apk)

## Project structure

The project is structured into packages with each package denoting a specific functionality within the app. These packages can be thought of as a *layer* that does specific things. The four main packages are

> * `app/data` 
> * `app/model` 
> * `app/ui` 
> * `app/util` 

#### data 

This package contains other sub-folders but it serves the main purpose of providing us with access to the core data required for the application. This includes access to the database, access to network to fetch more information about a bird, etc.

#### model 

The `model` folder holds POJOs that are stored in the database and used throughout the UI for tasks such as showing markers, filtering birds, etc.

#### ui 

The `ui` folder contains code for all the UI related code structured into sub-folders denoting the specific screen. This distinction helps keep the project clear to avoid confusion. Other UI specific classes such as Adapters (required to show information as a list) also go in this package.

#### util 

Finally, the `util` package contains some utility classes that are used multiple times throughout the project.
