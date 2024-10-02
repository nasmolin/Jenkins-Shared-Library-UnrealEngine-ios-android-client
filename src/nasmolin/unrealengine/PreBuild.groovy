package lab.nasmolin.unrealengine

class PreBuild implements Serializable {

    static final String REMOTE_USER = 'jenkins'

    /**
     * Метод проверяет unix-user'a, от которого будет выполняться пайплайн.
     * 
     * @param user          String, opt - Имя пользователя от которого запущен процесс(default: jenkins).
     * @param script        Script, req - Контекст DSL.
     */
    static void checkUser(Map args = [:]) {

        String desiredUser = args.user ?: REMOTE_USER
        args.script.println("[DEBUG] User must be ${desiredUser}")
        if (desiredUser == null){
            throw new Exception("[ERROR] @param user is null.")
        }

        String currentUser = args.script.sh(
                returnStdout: true, script: '#!/bin/bash\n set +e\n echo \$(whoami)').trim()
        args.script.println("[DEBUG] Current user is ${currentUser}")

        if (desiredUser != currentUser) {
            throw new Exception("[ERROR] The pipeline was launched from an incorrect user.")
        }
    }

    /**
     * Метод вывода в stdout информации о сборке из гит-тега.
     * Функция разбивает гит-тэг на группы и назначает в переменные tag, environment, version.
     * 
     * @param script        Script, req - Контекст DSL.
     */
    static void showBuildInfo(Map args = [:]){
        String tag = args.script.sh(returnStdout: true, script: '#!/bin/bash\n set +e\n echo \$TAG_NAME').trim()

        if (tag == null){
            throw new Exception("[ERROR] No git tag found, this build for tags only.")
        } else if (tag.matches(/(.+\/.+\/v.+)/)){

            def environment = args.script.sh(
                    returnStdout: true, script: "#!/bin/bash\nset +x\necho ${tag} | cut -d '/' -f2").trim()

            def version = args.script.sh(
                    returnStdout: true, script: "#!/bin/bash\nset +x\necho ${tag} | cut -d '/' -f3").trim()

            args.script.println "[DEBUG] # ================== Building project: ==================== #"
            args.script.println "[DEBUG] tag:${tag},environment:${environment},version:${version}"
            args.script.println "[DEBUG] # ========================================================= #"

        } else {
            throw new Exception("[ERROR] Git tag found, but not match regex.")
        }

    }

    /**
     * Метод определяет осуществляется ли сборка из гит-тега.
     * Так же проверяет есть ли совпадение имени тэга по регулярному выражению.
     * В случае совпадения, возвращает название окружения.
     * 
     * @param tag_name      Script, req - Гит-тэг.  
     * @return              String      - Имя окружения. def: default.
     */    
    static def setUpEnvByGitTag(tag_name) {
        if(tag_name.matches(/.+\/dev\/v([0-9]*).([0-9]*).([0-9]*)_([0-9]*)/)) {
            return 'dev'
        } else if(tag_name.matches(/.+\/staging\/v([0-9]*).([0-9]*).([0-9]*)_([0-9]*)/)) {
            return 'staging'
        } else {
            return 'default'
        }
    }
    /**
     * Метод возвращает stdout команды uname.
     *
     * @param script        Script, req - Контекст DSL.
     */
    static def getOS(script){
        String uname = script.sh(
                returnStdout: true, script: '#!/bin/bash\nset +x\nuname').trim()
        if (uname.startsWith('Darwin')) {
            return 'macos'
        } else if (uname.contains("Linux")) {
            return 'linux'
        } else {
            throw new Exception("[ERROR] Unsupported OS.")
        }
    }
}