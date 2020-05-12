# Team Rocket Weekly Report - Week 4 - 14.05.2020

## Goals from last week

*  Research on classification of birds and what categories to include by default. 
*  Decide on how to structure data such as the categories of the birds, their sighting times and locations.
*  Start working on writing an SQLite database to store bird sightings on the device.
*  Also to get familiar with uml class diagrams.

## Progress this week

The work that we have done as a collective this week consisted mostly of deciding data representation and structuring. After that, we divided the work of programming among ourselves.

* As stated in our goals from a week ago, we researched about what categories to include by default. The user can add custom categories to group the birds into but we have decided to have six categories as default - **<>**.
* We have also decided on what fields a ***Bird*** model object should contain. For now, we are storing the **name**, **family name** (analogous to category), **primary colour**, and its **size**. We have not yet decided on how to infer the *color* of a bird. The size we have declared to be either *Small*, *Medium*, or *Large*. The location and time of a sighting, along with the *Bird* object is present in the ***BirdSighting*** class.
* Regarding the UML class diagram of our project, we have decided on the structure of our project, specifically packages and classes, but the task of identifying relationships between various classes, interfaces, and objects still remains.
* We have created some user stories to make the app's features and navigation easier to understand and use without having to look at the code.

Moving to our individual tasks, we have focused first on adding features that are necessary for at least the minimal working of the app.

* Sreepradh worked on and completed the ability to capture image from the app and store the image file in the app's private storage. He has also added the ability to filter sightings in the map screen when a list item is clicked to show only bird of the same kind.
* Lokesh added a new *AddSightingActivity* to show the form where a user would be sent to to add a bird sighting. He has also been working on the UML class diagram implementation.
* Vasudev created a preliminary version of *AddSightingActivity* to store bird sighting data into the database and other tasks listed below.
    - He worked on handling cases when the user grants or denies permissions (specifically the *Location* permission), and auto-filling the location text field with the user location in case the permission is granted.
    - He added a listener to the database which notifies subscribed components whenever a new entry is added into the database so that other components such as the main list screen and map screen can update their data accordingly.
* Rahul has worked mainly on the map screen adding features as the database insert implementation neared usable state.
    - He wrote the code to show markers on the map corresponding to the bird sightings that are stored in the database.
    - Using the listener in the database, he has made it so that anytime a sighting is added the map updates the markers to show its location.

## Plans for next week

* Sreepradh and Lokesh will be implementing the ability to add categories to birds. There will be six predefined categories as stated in the previous section, and the user should be given the option to add more.
* Vasudev will work on implementing custom location and time selection while adding a bird sighting. As it is now, the device's location and time is used to auto-fill the fields and the user cannot edit those.
* We will have hopefully completed implementing all the essential features by the end of the next week. In the later weeks, we will test the app and move to adding necessary features according to the second phase.

## Meeting agenda

* Discuss with Rana about editing bird sighting details like time and location after having added it.
* Talk about how to know the color of a bird for filtering the list based on attributes.
* Talk about UML class diagram and how to change it according to changes in the project.
