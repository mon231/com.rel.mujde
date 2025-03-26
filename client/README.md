






// TODO: ensure no scripts are stored in sharedpref, all in dedicatedfolder


// TODO: move this code to service

// private List<String> readScriptContent(String scriptName) {
//     List<String> lines = new ArrayList<>();

//     log("Reading script: " + scriptName);

//     try {
//         // Look for the script in the app's files directory
//         File scriptsDir = new File(new File("/data/data/" + BuildConfig.APPLICATION_ID), "files/scripts");
//         File scriptFile = new File(scriptsDir, scriptName);

//         if (scriptFile.exists() && scriptFile.canRead()) {
//             try (BufferedReader reader = new BufferedReader(new FileReader(scriptFile))) {
//                 String line;
//                 while ((line = reader.readLine()) != null) {
//                     lines.add(line);
//                 }
//                 log("Successfully read script: " + scriptName + " with " + lines.size() + " lines");
//             }
//         } else {
//             log("Script file does not exist or is not readable: " + scriptFile.getAbsolutePath());

//             // Try using XSharedPreferences as a fallback
//             XSharedPreferences scriptPref = new XSharedPreferences(BuildConfig.APPLICATION_ID, "script_contents");
//             String scriptContent = scriptPref.getString(scriptName, "");

//             if (!scriptContent.isEmpty()) {
//                 String[] lineArray = scriptContent.split("\n");
//                 for (String line : lineArray) {
//                     lines.add(line);
//                 }
//                 log("Read " + lines.size() + " lines from XSharedPreferences");
//             }
//         }
//     } catch (Exception e) {
//         log("Error reading script: " + e.getMessage());
//     }

//     // Add a fallback message if the script couldn't be read
//     if (lines.isEmpty()) {
//         lines.add("// Unable to read script: " + scriptName);
//         lines.add("// Please place your script in: /data/data/" + BuildConfig.APPLICATION_ID + "/files/scripts/" + scriptName);
//         lines.add("// You can use 'adb push your_script.js /data/data/" + BuildConfig.APPLICATION_ID + "/files/scripts/" + scriptName + "'");
//     }

//     return lines;
// }
