
/**     
 * Экранируем пробелы и скобки при выборе xcode workspace по умолчанию.
 * Вообще было бы неплохо одной регуляркой это делать, а не тремя, и по всем спец-символам, а не только пробел и скобки. 
 * sh command: find . -iname "*(IOS).xcworkspace"  | sed 's/\ /\\ /g' | sed 's/(/\\(/g' | sed 's/)/\\)/g'
 * after snnipet generator:
 * (returnStdout: true, script: 'find . -iname "*(IOS).xcworkspace"  | sed \'s/\\ /\\\\ /g\' | sed \'s/(/\\\\(/g\' | sed \'s/)/\\\\)/g\'').trim()
 * 
 * Для получения списка схем и выбора первой схемы в списке
 * sh command: xcodebuild -list -workspace ${xcode_workspace} -hideShellScriptEnvironment | sed -n '/Schemes:/,+1 p' | awk '! /Schemes:$/'
 * after snnipet generator:
 * (returnStdout: true, script: "xcodebuild -list -workspace ${xcode_workspace} -hideShellScriptEnvironment | sed -n \'/Schemes:/,+1 p\' | awk \'! /Schemes:\$/\'").trim()
 */

class XCodeBuilder implements Serializable {

    /**
     * Метод очистки кэша Xcode, 
     * для очистки используется скрипт вместо 'xcodebuild -clean' из-за ограничений связанных с UE.
     * 
     * @param script            Script, req - Контекст DSL
     */
    static void cleanCache(Map args = [:]) {
        args.script.println "[START] Cleaning XCode cache."
        args.script.sh '''
                    export IS_XCODE_CACHE_FOLDER_PRESENT="`ls ~/Library/Developer/ | grep "Xcode" | wc -l`"
                    if [ 0 -ne "$IS_XCODE_CACHE_FOLDER_PRESENT" ]; then
                      echo "[PROGRESS] Xcode cache folder(~/Library/Developer/Xcode) should not be present at build time! Attempting to delete..."
                      rm -rf ~/Library/Developer/Xcode
                      RM_RESULT=$?
                        if [ 0 -ne "$RM_RESULT" ]; then
                          echo "[FAILED] remove ~/Library/Developer/Xcode folder!"
                          exit $RM_RESULT
                        else
                          echo "[SUCCESSFUL] remove ~/Library/Developer/Xcode folder!"
                        exit 0
                        fi
                    else
                      echo "[SKIP] Xcode cache folder(~/Library/Developer/Xcode) not found, nothing to delete. Skiping..."
                      exit 0
                    fi
                    '''
        args.script.sh '''
                    export IS_XCODE_CACHE_FOLDER_PRESENT="`ls ~/Library/Caches/ | grep "com.apple.dt.Xcode" | wc -l`"
                    if [ 0 -ne "$IS_XCODE_CACHE_FOLDER_PRESENT" ]; then
                      echo "[PROGRESS] Xcode cache folder(~/Library/Caches/com.apple.dt.Xcode) should not be present at build time! Attempting to delete..."
                      rm -rf ~/Library/Caches/com.apple.dt.Xcode
                      RM_RESULT=$?
                        if [ 0 -ne "$RM_RESULT" ]; then
                          echo "[FAILED] remove ~/Library/Caches/com.apple.dt.Xcode folder!"
                          exit $RM_RESULT
                        else
                          echo "[SUCCESSFUL] remove ~/Library/Caches/com.apple.dt.Xcode folder!"
                        exit 0
                        fi
                    else
                      echo "[SKIP] Xcode cache folder(~/Library/Caches/com.apple.dt.Xcode) not found, nothing to delete. Skiping..."
                      exit 0
                    fi
                    '''
    }   
    
    /**
     * Метод возвращает список доступных для сборки схем из .xcworkspace.
     * По умолчанию xcode workspace определятся из корневой сборочной директории по префиксу *(IOS).xcworkspace
     * 
     * @param script            Script, req - Контекст DSL
     * @param xcode_workspace   String, opt - Имя .xcworkspace файла, default: *(IOS).xcworkspace.
     */
    static void listScheme(Map args = [:]) {
        String xcode_workspace              = args.xcode_workspace ?: args.script.sh (returnStdout: true, script: 'find . -iname "*(IOS).xcworkspace"  | sed \'s/\\ /\\\\ /g\' | sed \'s/(/\\\\(/g\' | sed \'s/)/\\\\)/g\'').trim()
        
        args.script.println "[START] Parsing XCode schemes. Workspace:${xcode_workspace}"
        args.script.sh "xcodebuild -list -workspace ${xcode_workspace} -hideShellScriptEnvironment"
    }

    /**
     * Метод собирает билд из указанной схемы
     * 
     * @param script            String, req - Контекст DSL
     * @param xcode_scheme      String, opt - Имя схемы для сборки.
     * @param xcode_workspace   String, opt - Имя .xcworkspace файла, default: *(IOS).xcworkspace.
     */
    static void buildScheme(Map args = [:]) {
        String xcode_workspace              = args.xcode_workspace ?: args.script.sh (returnStdout: true, script: 'find . -iname "*(IOS).xcworkspace"  | sed \'s/\\ /\\\\ /g\' | sed \'s/(/\\\\(/g\' | sed \'s/)/\\\\)/g\'').trim()
        String xcode_scheme                 = args.xcode_scheme ?: args.script.sh (returnStdout: true, script: "xcodebuild -list -workspace ${xcode_workspace} -hideShellScriptEnvironment | sed -n \'/Schemes:/,+1 p\' | awk \'! /Schemes:\$/\'").trim()

        args.script.println "[START] Building XCode. Scheme:${xcode_scheme}, workspace:${xcode_workspace}"
        args.script.sh """xcodebuild build \
                            -scheme ${xcode_scheme} \
                            -workspace ${xcode_workspace} \
                            -sdk iphoneos -destination generic/platform=iOS \
                            -hideShellScriptEnvironment
                        """
    }

    /**
     * Метод для архивация приложения(.xcarchive).
     * 
     * @param script                        Script, req - Контекст DSL
     * @param xcode_scheme                  String, opt - Имя схемы для сборки.
     * @param xcode_workspace               String, opt - Имя .xcworkspace файла, default: *(IOS).xcworkspace.
     * @param xcode_output_appstore_path    String, opt - Путь до xcode output директории app-store окружения, default ./_build/_xcode/_output/_app-store.
     */
    static void generateArchive(Map args = [:]) {
        String xcode_workspace              = args.xcode_workspace ?: args.script.sh (returnStdout: true, script: 'find . -iname "*(IOS).xcworkspace"  | sed \'s/\\ /\\\\ /g\' | sed \'s/(/\\\\(/g\' | sed \'s/)/\\\\)/g\'').trim()
        String xcode_scheme                 = args.xcode_scheme ?: args.script.sh (returnStdout: true, script: "xcodebuild -list -workspace ${xcode_workspace} -hideShellScriptEnvironment | sed -n \'/Schemes:/,+1 p\' | awk \'! /Schemes:\$/\'").trim()
        String xcode_output_appstore_path   = args.xcode_output_appstore_path ?: './_build/_xcode/_output/_app-store'
        
        args.script.println "[START] Create XCode archive. Scheme:${xcode_scheme}, workspace:${xcode_workspace}, output:${xcode_output_appstore_path}/${xcode_scheme}.xcarchive"
        args.script.sh """xcodebuild archive \
                            -scheme ${xcode_scheme} \
                            -workspace ${xcode_workspace} \
                            -sdk iphoneos -destination generic/platform=iOS \
                            -archivePath ${xcode_output_appstore_path}/${xcode_scheme}.xcarchive \
                            -hideShellScriptEnvironment
                        """
    }

    /**
     * Генерация переменных для экспорта, конфигурирование info.plist файла.
     * По умолчанию редактируется info.plist из архива по пути ./_build/_xcode/_output/_app-store/${xcode_scheme}.xcarchive/info.plist.
     * 
     * @param script                            Script, req - Контекст DSL
     * @param xcode_cfbundleversion             String, opt - Номер билда(default: 'date "+%Y%m%d%H%M%S"')
     * @param xcode_infoplist_appstore_path     String, opt - Путь до info.plist файла, default: ./_build/_xcode/_output/_app-store/${xcode_scheme}.xcarchive/info.plist.
     */
    static void setInfoPlist(Map args = [:]) {
        String xcode_workspace                  = args.xcode_workspace ?: args.script.sh (returnStdout: true, script: 'find . -iname "*(IOS).xcworkspace"  | sed \'s/\\ /\\\\ /g\' | sed \'s/(/\\\\(/g\' | sed \'s/)/\\\\)/g\'').trim()
        String xcode_scheme                     = args.xcode_scheme ?: args.script.sh (returnStdout: true, script: "xcodebuild -list -workspace ${xcode_workspace} -hideShellScriptEnvironment | sed -n \'/Schemes:/,+1 p\' | awk \'! /Schemes:\$/\'").trim()
        String xcode_cfbundleversion            = args.xcode_cfbundleversion ?: args.script.sh (returnStdout: true, script: 'date "+%Y%m%d%H%M%S"').trim()
        String xcode_output_appstore_path       = args.xcode_output_appstore_path ?: './_build/_xcode/_output/_app-store'
        String xcode_infoplist_appstore_path    = args.xcode_infoplist_appstore_path ?: "${xcode_output_appstore_path}/${xcode_scheme}.xcarchive/info.plist"
        
        args.script.println "[START] Editing Info.plist, setting CFBundleVersion. Info.plist path:${xcode_infoplist_appstore_path}, CFBundleVersion:${xcode_cfbundleversion}"
        args.script.sh """/usr/libexec/PlistBuddy -c \
                        "Add ApplicationProperties:CFBundleVersion string ${xcode_cfbundleversion}" \
                        "${xcode_infoplist_appstore_path}" 
                        """
        args.script.sh "cat ${xcode_infoplist_appstore_path}"
    }

    /**
     * Экспорт архива в TestFlight.
     * 
     * @param script                            Script, req - Контекст DSL
     * @param xcode_output_appstore_path        String, opt - Путь до xcode output директории app-store окружения, default ./_build/_xcode/_output/_app-store.
     */
    static void exportArchive(Map args = [:]) {    
        String xcode_workspace                  = args.xcode_workspace ?: args.script.sh (returnStdout: true, script: 'find . -iname "*(IOS).xcworkspace"  | sed \'s/\\ /\\\\ /g\' | sed \'s/(/\\\\(/g\' | sed \'s/)/\\\\)/g\'').trim()
        String xcode_scheme                     = args.xcode_scheme ?: args.script.sh (returnStdout: true, script: "xcodebuild -list -workspace ${xcode_workspace} -hideShellScriptEnvironment | sed -n \'/Schemes:/,+1 p\' | awk \'! /Schemes:\$/\'").trim()        
        String xcode_output_appstore_path       = args.xcode_output_appstore_path ?: './_build/_xcode/_output/_app-store'
        String xcode_infoplist_appstore_path    = args.xcode_infoplist_appstore_path ?: "${xcode_output_appstore_path}/${xcode_scheme}.xcarchive/info.plist"       
        String tag_name = args.script.sh (returnStdout: true, script: 'git describe --exact-match --tags').trim()
        String xcode_exportplist_appstore_path
        def environment = new PreBuild().setUpEnvByGitTag(tag_name)
        
        if (environment == 'ios/dev') {
        xcode_exportplist_appstore_path = "${xcode_output_appstore_path}/ExportOptions-AppStore-internal.plist"
        }
        if (environment == 'ios/staging') {
        xcode_exportplist_appstore_path = "${xcode_output_appstore_path}/ExportOptions-AppStore.plist"
        }
        if (environment == 'ios/prod') {
        xcode_exportplist_appstore_path = "${xcode_output_appstore_path}/ExportOptions-AppStore.plist"
        }
        if (environment == 'no_tag') {
        xcode_exportplist_appstore_path = "${xcode_output_appstore_path}/ExportOptions-AppStore-internal.plist"
        }
        if (environment == "default") {
        xcode_exportplist_appstore_path = "${xcode_output_appstore_path}/ExportOptions-AppStore-internal.plist"
        }
        args.script.println "[DEBUG] Git tag: " + tag_name
        args.script.println "[DEBUG] For export archive will be used ExportOptions.plist for: " + environment + " environment"
        args.script.println "[DEBUG] Export Xcode archive. Info.plist path:${xcode_infoplist_appstore_path}, export.plist path:${xcode_exportplist_appstore_path}, scheme:${xcode_scheme}, workspace:${xcode_workspace}, output:${xcode_output_appstore_path}/${xcode_scheme}.xcarchive"
        args.script.sh """xcodebuild -exportArchive \
                            -archivePath "${xcode_output_appstore_path}/${xcode_scheme}.xcarchive" \
                            -exportOptionsPlist "${xcode_exportplist_appstore_path}" \
                            -exportPath "${xcode_output_appstore_path}" \
                            -allowProvisioningUpdates \
                            -hideShellScriptEnvironment
                        """

    }

}