
class PreBuild implements Serializable {

    /**
     * Метод проверяет unix-user'a, от которого будет выполняться пайплайн.
     * 
     * @param user          String, opt - Имя пользователя от которого запущен процесс(default: jenkins).
     * @param script        Script, req - Контекст DSL.
     */
    static void checkUser(Map args = [:]) {
        String user = args.user ?: "Jenkins"
        args.script.sh  """
            echo 'User must be ${user}'
            if [ \$(whoami) == ${user} ]
            then 
                echo 'OK: user = ${user}.'
                exit 0
            else
                echo 'ERROR: user != ${user}.'
                echo 'current user is: \$(whoami))'
                exit 1
            fi
            """
    }

    static void os(Map args = [:]){
        String uname = args.script.sh (returnStdout: true, script: 'uname').trim()
        args.script.println "[DEBUG] Uname is: ${uname}"
        return uname
    }
    
    /**
     * Метод определяет осуществляется ли сборка из гит-тега.
     * Так же проверяет есть ли совпадение имени тэга по регулярному выражению.
     * В случае совпадения, возвращает название окружения.
     * 
     * @param tag_name      Script, req - Гит-тэг.  
     * @return              String      - Имя окружения. def: default.
     */    
    static void setUpEnvByGitTag(tag_name) {
        if (tag_name == null) {
            return 'no_tag'
        } else {
            if (tag_name ==~ /ios\/dev\/v([0-9]*).([0-9]*).([0-9]*)_([0-9]*)/) {
                return 'ios/dev'
            }
            if (tag_name ==~ /ios\/staging\/v([0-9]*).([0-9]*).([0-9]*)_([0-9]*)/) {
                return 'ios/staging'
            }
            if (tag_name ==~ /ios\/prod\/v([0-9]*).([0-9]*).([0-9]*)_([0-9]*)/) {
                return 'ios/prod'
            } 
            if (tag_name ==~ /android\/dev\/v([0-9]*).([0-9]*).([0-9]*)_([0-9]*)/) {
                return 'android/dev'
            }
            return 'default'
        }
    }
}
