Immediate attention:
Implement tooltip properly for every icon, if the mouse is placed over, it should stay displaying but now it is shown and then hide even if the mouse is there. Also there is no subtle disappearing animation / transition, currently the tooltip is disappearing abruptly, creating disturbance (Critical).
Improvements:
The icon is not visible in the taskbar when the run icon from IDE gutter is hit. Also I don't know about the installers (exe, dmg, deb, rpm) package icon and taskbar icon (app-icon.svg).

To be verified manually ensure functionality:
The configured database loading of users and libraries should happen while the application starting. If the user or library data is not found, then it should use the file. Also if the database is empty, the application should show the data from the file. The UI should not show empty screens.
Where are the library name and branch are stored (in file/db/both) because db is preferred, so everyone globally get the library list equally in all location and device? 
Did you add the import from db in initial setup (configuration)?
Pay later persistence in database should be verified.
Logging must be verified across all OS.