name: '🐞 Bug report'
description: Reporting an issue
labels: [bug, needs triage]

body:
- type: checkboxes
  id: checks
  attributes:
    label: Checks
    options:
    - label: I have checked that this issue has not already been reported (even among closed issues).
      required: true
    - label: I have confirmed this bug exists on the [latest version](https://www.spigotmc.org/resources/graves.116202/history).
      required: true

- type: input
  id: plugin-version
  attributes:
    label: Version
    description: Version of your installed plugin (latest is not a version)
  placeholder: 4.9.x
  validations:
    required: true

- type: input
  id: server-version
  attributes:
  label: Server version
  description: Version of your minecraft server (latest is not a version)
  placeholder: 1.20.x
  validations:
    required: true

- type: textarea
  id: plugin-list
  attributes:
    label: Plugin list
    description: |
      All plugins running on your server you're experiencing this issue on.
      Use `/plugins` to list plugins on your backend server.
  validations:
    required: true

- type: textarea
  id: expected-behaviour
  attributes:
    label: Expected behavior
    description: What you expected to work and how.
  validations:
    required: true

- type: textarea
  id: actual-behaviour
  attributes:
    label: Actual behavior
    description: What actually happens.
  validations:
    required: true

- type: textarea
  id: stacktrace
  attributes:
    label: Stacktrace if applicable (no screenshots!)
    description: Copy/paste this out of your server logs.
  validations:
    required: false

- type: textarea
  id: additional-information
  attributes:
    label: Additional information
  validations:
    required: false
