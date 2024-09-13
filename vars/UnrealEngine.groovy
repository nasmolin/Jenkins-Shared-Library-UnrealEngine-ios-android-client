//package pro.iddqd.UnrealEngine

class UnrealEngine implements Serializable {
    
    /**
     *
     * Unreal-Engine 5.3.2 
     * Функция инициализирует сборку android-клиента через:
     * RunUAT.sh BuildCookRun [ -flags ]
     * переодичски пайплайн может зависнуть намертво на этапе UE COOK.
     * по моим наблюдениям такого не случается после сборки с параметром -cookonthefly
     * поэтому мы сначала собираем с параметром -cookonthefly, после с -cook.
     * 
     *
     * @param script                    Script, req - Контекст DSL.
     * @param ue_batch_files_path       Script, opt - путь до каталога со скриптами сборки.
     * @param ue_uproject_path          String, opt - путь до файл проекта для сборки, по умолчанию: echo $(find . -iname "*.uproject").
     * @param ue_output_path            String, opt - output директория сборки.
     */
    static void buildCookRun(Map args = [:]){
        String tag_name = args.script.sh (returnStdout: true, script: 'git describe --exact-match --tags').trim()
        String platform = args.script.sh (returnStdout: true, script: "echo ${tag_name} | cut -d '/' -f1").trim()
        String ue_output_path = args.script.sh (returnStdout: true, script: 'echo \$(pwd)/_build/_ue/_output').trim()
        String ue_uproject_path = args.script.sh (returnStdout: true,
            script: 'echo \$(pwd)\$(find . -maxdepth 1 -iname "*.uproject" | sed "s/^.//")').trim()
        String ue_batch_files_path = args.ue_batch_files_path ?: null

        args.script.sh """
            ${ue_batch_files_path}/RunUAT.sh \
            BuildCookRun -project="${ue_uproject_path}" \
            -clientconfig=Shipping -nodebuginfo -nocompile -nocompileuat \
            -archive -package -build -pak -iostore -compressed -prereqs \
            -archivedirectory="${ue_output_path}" -platform="${platform}" \
            -target=ArenaPrototype_2 -nop4 -utf8output -clean -cookonthefly -stage     
            """
        args.script.sh """
            ${ue_batch_files_path}/RunUAT.sh \
            BuildCookRun -project="${ue_uproject_path}" \
            -clientconfig=Shipping -nodebuginfo -nocompile -nocompileuat \
            -archive -package -build -pak -iostore -compressed -prereqs \
            -archivedirectory="${ue_output_path}" -platform="${platform}" \
            -target=ArenaPrototype_2 -nop4 -utf8output -clean -cook -stage     
            """
    }

    /**
     * Функция генерирует xcode workspace файлы в корневой директории проекта.
     *
     * @param script                    Script, req - Контекст DSL.     
     * @param ue_batch_files_path       Script, opt - путь до каталога со скриптами сборки.
     */
    
    static void generateXcodeFiles(Map args = [:]){
        String ue_batch_files_path = args.ue_batch_files_path ?: "/Users/Shared/EpicGames/UE_5.3/Engine/Build/BatchFiles"
        String ue_uproject_path = args.script.sh (returnStdout: true,
            script: 'echo \$(pwd)\$(find . -maxdepth 1 -iname "*.uproject" | sed "s/^.//")').trim()

        args.script.sh """${ue_batch_files_path}/Mac/GenerateProjectFiles.sh \
            -project="${ue_uproject_path}" \
            -game
            """
    }

    /**
     *
     * Функции определения переменных из гит-тэга для дальнейшей сборки(WIP):
     * ex.:  android/staging/v1.2.3_123 or ios/dev/v1.2.3_123
     *
    static void defineTagName(){
        return sh (returnStdout: true, script: 'git describe --exact-match --tags').trim()
    }
    static void definePlatform(){
        def tag_name = new UnrealEngine().defineTagName()          
        return sh (returnStdout: true, script: "echo ${tag_name} | cut -d '/' -f1").trim()
    }
    static void defineEnvironment(){
        def tag_name = new UnrealEngine().defineTagName()          
        return sh (returnStdout: true, script: "echo ${tag_name} | cut -d '/' -f2").trim()
    }
    static void defineVersion(){
        def tag_name = new UnrealEngine().defineTagName()          
        return sh (returnStdout: true, script: "echo ${tag_name} | cut -d '/' -f3").trim()
    }
    /

}
