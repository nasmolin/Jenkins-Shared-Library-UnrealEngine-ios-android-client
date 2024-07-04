class UnrealEngine implements Serializable {
    
    /**
     *
     * Unreal-Engine 5.3.2 
     * Функция инициализирует сборку ios-клиента через:
     * RunUAT.sh BuildCookRun [ -flags ]
     *
     * @param script                    Script, req - Контекст DSL
     * @param ue_batch_files_path       Script, opt - путь до каталога со скриптами сборки.
     * @param ue_uproject_path          String, opt - путь до файл проекта для сборки, по умолчанию: echo $(find . -iname "*.uproject").
     * @param ue_output_path            String, opt - output директория сборки.
     */
    static void clientIOS(Map args = [:]) {
        String ue_batch_files_path  = args.ue_batch_files_path ?: "/Users/Shared/EpicGames/UE_5.3/Engine/Build/BatchFiles"
        String ue_uproject_path     = args.ue_uproject_path ?: args.script.sh (returnStdout: true, script: 'echo \$(pwd)\$(find . -maxdepth 1 -iname "*.uproject" | sed "s/^.//")').trim()
        String ue_output_path       = args.ue_output_path ?: args.script.sh (returnStdout: true, script: 'echo \$(pwd)/_build/_ue/_output').trim()

        // Запуск компиляции Unreal-Engine проекта под IOS-Client
        args.script.sh """${ue_batch_files_path}/RunUAT.sh \
            BuildCookRun -project="${ue_uproject_path}" \
            -clientconfig=Shipping -nodebuginfo -nocompile -nocompileuat \
            -archive -package -build -pak -iostore -compressed -prereqs \
            -archivedirectory="${ue_output_path}" -platform=IOS \
            -target=ArenaPrototype_2 -nop4 -utf8output -clean -cook -stage 
            """
      // Генерация XCode проекта из Unreal-Engine скрипта
        args.script.sh """${ue_batch_files_path}/Mac/GenerateProjectFiles.sh \
            -project="${ue_uproject_path}" \
            -game
            """
    }
}
