# Team Rocket Weekly Report - Week 4 - 14.05.2020

## Goals from last week

*  Research on classification of birds and what categories to include by default. 
*  Decide on how to structure data such as the categories of the birds, their sighting times and locations.
*  Start working on writing an SQLite database to store bird sightings on the device.
*  Also to get familiar with uml class diagrams.

## Progress this week

### Collective tasks

* As stated in our goals from a week ago, we researched about what categories to include by default. The user can add custom categories to group the birds into but we have decided to have six categories as default.
* We have also decided on what fields a *Bird* model object should contain. For now, we are storing the fields listed below.

    > * Name
    > * Family name (same as Category)
    > * Primary colour 
    > * Size.
    
    We have not yet decided on how to infer the *color* of a bird. The size we have declared to be either *Small*, *Medium*, or *Large*. 
    
    The location and time of a sighting, along with the *Bird* object is present in the *BirdSighting* class.

* Regarding the UML class diagram of our project, we have decided on the structure of our project, specifically packages and classes, but the task of identifying relationships between various classes, interfaces, and objects still remains.

### Individual tasks

#### Sreepradh

* Implemented capturing image from the app and save the image file in the app's private storage. 
* Added the ability to filter sightings to show only the same birds' when a list item is clicked.

#### Lokesh

* Added a new activity, *AddSightingActivity*, to show the form where a user would be sent to to add a bird sighting. 
* Worked on the UML class diagram implementation for the project.

#### Vasudev

* Created a basic initial design of *AddSightingActivity* to store bird sighting data into the database.
* Worked on handling cases when the user grants or denies permissions (specifically the *Location* permission), and auto-filling the location text field in case the permission is granted.
* Added a listener to the database which notifies subscribed components whenever a new entry is added into the database so that they can update their data accordingly, without having to fetch all the saved data once more.

#### Rahul

* Worked on showing map markers for all bird sightings present in the database.
* Updating map markers to show any new added sighting automatically using the database listener.

## Plans for next week

* Sreepradh and Lokesh will be implementing the ability to add categories to birds. There will be six predefined categories as stated in the previous section, and the user should be given the option to add more.
* Vasudev will work on implementing custom location and time selection while adding a bird sighting. As it is now, the device's location and time is used to auto-fill the fields and the user cannot edit those.
* Rahul will be working on implementation of UML class diagrams.
* We will have hopefully completed implementing all the essential features by the end of the next week. In the later weeks, we will test the app and move to adding necessary features according to the second phase.

## Meeting agenda

* Discuss with Rana about editing bird sighting details like time and location after having added it.
* Talk about how to know the color of a bird for filtering the list based on attributes.
* Talk about UML class diagram and how to change it according to changes in the project.
