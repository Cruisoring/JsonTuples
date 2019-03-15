package Utilities;

import com.google.common.base.Charsets;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper class for resource retrieval.
 */
public class ResourceHelper {

    public final static String[] resourcePaths;
    static {
        resourcePaths = getResourcePaths();

        String firstPath = resourcePaths[0];
        firstPath = firstPath.substring(0, firstPath.indexOf("src"));
    }

    /**
     * Retrive the ORIGINAL resources folders of all modules involved with the call
     * @return  String array identifying the absolute paths of related resource folders
     */
    private static String[] getResourcePaths(){
        List<String> classPaths = new ArrayList<>();
        List<String> classNames = getCallerClassNames();
        for(int i = 0; i < classNames.size(); i++){
            try{
                String className = classNames.get(i);
                Class clazz = Class.forName(className);
                String classPath = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();

                if(classPath.endsWith("target/classes/")){
                    classPath = classPath.replace("target/classes/", "src/main/resources/");
                } else if (classPath.endsWith("target/test-classes/")) {
                    classPath = classPath.replace("target/test-classes/", "src/test/resources/");
                } else {
                    continue;
                }

                if(!classPaths.contains(classPath)){
                    classPaths.add(classPath);
                }
            }catch (Exception ex){
                continue;
            }
        }
        Collections.reverse(classPaths);
        String[] result = classPaths.toArray(new String[0]);
        return result;
    }


    /**
     * Get the caller class who calls any methods of the baseTestRunner
     * @return Class of the Caller.
     */
    public static List<String> getCallerClassNames(){
        StackTraceElement[] stacks = new Throwable().getStackTrace();
        List<String> classNames = Arrays.stream(stacks).skip(1).map(stack -> stack.getClassName()).collect(Collectors.toList());

        return classNames;
    }

    /**
     * Get the caller class who calls any methods of the baseTestRunner
     * @return Class of the Caller.
     */
    public static Class getCallerClass(){
        List<String> callers = getCallerClassNames();
        String thisClassName = ResourceHelper.class.getName();
        try{
            String externalCaller = callers.stream().filter(caller -> !caller.contains(thisClassName)).findFirst().orElse(null);
            Class callerClass = Class.forName(externalCaller);
            return callerClass;
        } catch (Exception ex){
            return null;
        }
    }

    /**
     * Retrieve the solic file from any possible module.
     * @param filename Name of the file to be handled.
     * @param folderNames Directory names of the file.
     * @return File instance if it exist, otherwise null.
     */
    public static File getResourceFile(String filename, String... folderNames){
        Objects.requireNonNull(filename);

        String folderPath = folderNames == null ? "" : String.join("/", folderNames);

        //Try to load properties if it is not loaded by previous resourcePaths.
        for (String path : resourcePaths) {
            File resourceFolder = new File(path, folderPath);
            File file = new File(resourceFolder, filename);
            //No such resources defined, continue
            if (!file.exists()){
                continue;
            }

            return file;
        }

        return null;
    }

    /**
     * Locate the resource identified with filename and its folder names from any possible module.
     * @param filename Name of the file to be handled.
     * @param folderNames Directory names of the file.
     * @return the absolute file path if it is found, or null when there is no such resource.
     */
    public static Path getResourcePath(String filename, String... folderNames){
        Objects.requireNonNull(filename);

        String folderPath = folderNames == null ? "" : String.join("/", folderNames);

        //Try to load properties if it is not loaded by previous resourcePaths.
        for (String path : resourcePaths) {
            File resourceFolder = new File(path, folderPath);
            File file = new File(resourceFolder, filename);
            //No such resources defined, continue
            if (!file.exists()){
                continue;
            }

            return file.toPath();
        }

        String error = String.format("Failed to locate %s in folder of %s from %s", filename, folderPath, String.join(",", resourcePaths));
        throw new RuntimeException(error);
    }

    /**
     * Retrieve the absolute file path from any possible module.
     * @param filename Name of the file to be handled.
     * @param folderNames Directory names of the file.
     * @return Path of the expected file.
     */
    public static Path getAbsoluteFilePath(String filename, String... folderNames){
        Objects.requireNonNull(filename);

        String folderPath = folderNames == null ? "" : String.join("/", folderNames);

        //Output Folder in the original caller module target directory
        File folder = new File(resourcePaths[0], folderPath);

        File file = new File(folder, filename);
        return file.toPath();
    }

    /**
     * Check to see if there is a relative resource identified by the resourceFilename.
     * @param resourceFilename  The relative path of the reourcefile to be checked.
     * @return  'True' if the relative path exists, "False" if not.
     */
    public static Boolean isResourceAvailable(String resourceFilename){
        return getResourcePath(resourceFilename) != null;
    }

    /**
     * Retrieve the content of the resource file a String.
     * @param resourceFilename The relative path of the reourcefile to be checked.
     * @param folders Optional folder names.
     * @return NULL if there is no such resource identified by the relative path, or content of the resource as a String.
     */
    public static String getTextFromResourceFile(String resourceFilename, String... folders){
        Path path = getResourcePath(resourceFilename, folders);
//        log.info(String.format("%s would be extracted from %s", resourceFilename, path));

        if(path == null){
            return null;
        }

        try {
            byte[] encoded = Files.readAllBytes(path);
            String text = new String(encoded, Charsets.UTF_8);
            return text;
        } catch (IOException e) {
            return null;
        }
    }
}
