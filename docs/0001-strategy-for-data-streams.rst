Title: Strategy for asynchronous data streams in the OpenEdx Project
==================================================
Date: 14 November 2023

Status
------
Accepted

Context
------
In the OpenEdx project, we are developing a mobile application using a Kotlin language for Android
users. To ensure optimal support of the application, we need to make a decision regarding which
asynchronous data streams will be used for the future Android app development. This document
outlines the decision to support native Kotlin based StateFlow and SharedFlow.

Decision
------
We decide to use StateFlow and SharedFlow for asynchronous data streams management between UI and view
models. All new features should use flows instead of LiveData. All LiveData occurrences in current code
should be replaced with flows in future.

Why is this important?

1. Deep Kotlin integration
------
Flow is tightly integrated with Kotlin Coroutines. Code parts which are using Kotlin Coroutines is
usual use flows, we don't need to map data to another format and could use it as is.

2. Decreasing dependencies count
------
If we are using language based choose, we don't need to add additional dependencies to our code.

Project Impact
------

This decision will impact the project in the following ways:
------
Improved performance and functionality.
Reducing application size.

Implementation
------
To implement this decision, we will use flows as asynchronous data streams for future development.
We will replace LiveData with flows during refactoring

Alternatives
------
Continuing to use LiveData as asynchronous data streams, will keep job to us to maintain LiveData
library updates.