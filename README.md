# Tempo Importer

Supports importing [Hamster](https://github.com/projecthamster/hamster) XML exports into JIRA Tempo

Compatibility Matrix

| System  | Version |
|---------|---------|
| JIRA    | 9.6.0   |
| Hamster | 3.0.2   |


## Usage

Run `Main` class.
Provide env variables:

| name              | description                                                                                                                                           |
|-------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
 | JIRA_BASE_URI     | the base uri of your jira instance                                                                                                                    |
| JIRA_TEMPO_WORKER | the worker for which the worklogs are to be imported (your user name usually)                                                                         |
| JIRA_PAT          | a [Jira Personal Access Token](https://confluence.atlassian.com/enterprise/using-personal-access-tokens-1026032365.html) to be used for authorization |

Add single argument pointing to an XML hamster export.

## Development Ideas

[ ] Synchronize with Tempo instead of importing
[ ] Retrieve activities from Hamster using DBus interface
[ ] Build a runnable application
