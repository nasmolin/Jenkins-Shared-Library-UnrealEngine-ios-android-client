package lab.nasmolin.unrealengine
 
/**
 * Класс для разблокировки MacOS Keychain.
 *   при подключении по ssh системное хранилище ключей остается закрытым, для обхода
 *   используется временный keychain в каторый экспортируктся сертификат разработчика apple.
 *
 * подробнее описано тут: 
 *   1. https://developer.apple.com/forums/thread/712005,
 */
class KeychainUnlocker implements Serializable {
    
    /**
     * Метод создания временного хранилища ключей
     *
     * @param temp_keychain             String, req - имя временного хранилища ключей.
     * @param temp_keychain_pass        String, req - пароль от временного хранилища ключей.
     */
    static void createTempKeychain(Map args = [:]) {
        String temp_keychain            = args.temp_keychain
        String temp_keychain_pass       = args.temp_keychain_pass

        args.script.println "[DEBUG] Temp keychain will be created."
        args.script.println "[DEBUG] temp_keychain = ${temp_keychain}"
        args.script.println "[DEBUG] temp_keychain_pass = ${temp_keychain_pass}"
                            
        args.script.sh "security create-keychain -p ${temp_keychain_pass} ${temp_keychain}"
    }

    /**
     * Для добавления в список временного keychain.
     *
     * @param temp_keychain             String, req - имя временного хранилища ключей.
     */
    static void appendTempKeychain(Map args = [:]) {
        String temp_keychain            = args.temp_keychain 
        
        args.script.println "[DEBUG] Append keychain to the search list."        
        args.script.println "[DEBUG] temp_keychain = ${temp_keychain}"

        args.script.sh """security list-keychains -d user -s ${temp_keychain} \$(security list-keychains -d user | sed s/\\"//g)"""
        args.script.sh "security set-keychain-settings ${temp_keychain}"
    }  

    /**
     * Метод для разблокировки временного keychain.
     *
     * @param login_keychain                String, req - путь до login хранилища ключей.
     * @param login_keychain_pass           String, req - пароль от login хранилища ключей. 
     * @param temp_keychain                 String, req - имя временного хранилища ключей.
     * @param temp_keychain_pass            String, req - пароль от временного хранилища ключей.
     * @param temp_keychain_unlock_timeout  String, opt - тайм-аут для повторной блокировки хранилища ключей в сек., default: 1час. 
     */
    static void unlockKeychains(Map args = [:]) {
        String temp_keychain                    = args.temp_keychain
        String temp_keychain_pass               = args.temp_keychain_pass
        String temp_keychain_unlock_timeout     = args.temp_keychain_unlock_timeout ?: '3600'
        String login_keychain                   = args.login_keychain
        String login_keychain_pass              = args.login_keychain_pass

        args.script.println "[DEBUG] Unlock the keychain."        
        args.script.println "[DEBUG] temp_keychain = ${temp_keychain}"
        args.script.println "[DEBUG] temp_keychain_unlock_timeout = ${temp_keychain_unlock_timeout}"
        args.script.println "[DEBUG] login_keychain = ${login_keychain}"
                            
        args.script.sh "security unlock-keychain -p ${login_keychain_pass}"
        args.script.sh "security unlock-keychain -p ${temp_keychain_pass} ${temp_keychain}"
        args.script.sh "security unlock-keychain -p ${login_keychain_pass} ${login_keychain}"
        args.script.sh "security set-keychain-settings -lut ${temp_keychain_unlock_timeout} ${temp_keychain}"
    }  

    /**
     * Метод для импорта сертификата разработчика apple во временный keychain.
     *
     * @param cert_path                 Script, req - путь до сертификата apple developer в формате .p12.
     * @param temp_keychain             String, req - имя временного хранилища ключей.
     * @param temp_keychain_pass        String, req - пароль от временного хранилища ключей.
     */
    static void certImportTempKeychain(Map args = [:]) {
        String cert_path                = args.cert_path
        String temp_keychain            = args.temp_keychain
        String temp_keychain_pass       = args.temp_keychain_pass

        args.script.println "[DEBUG] Import certificate to temp keychain"
        args.script.println "[DEBUG] cert_path = ${cert_path}"
        args.script.println "[DEBUG] temp_keychain = ${temp_keychain}"
        args.script.println "[DEBUG] temp_keychain_pass = ${temp_keychain_pass}"
                   
        args.script.sh "security import ${cert_path} -k ${temp_keychain} -P ${temp_keychain_pass} -T '/usr/bin/codesign'"
    }

    /**
     * Метод отображения информации о разработчике apple.
     *
     * @param temp_keychain             String, req - имя временного хранилища ключей.
     */
    static void infoDeveloperIdentity(Map args = [:]) {
        String temp_keychain            = args.temp_keychain
        
        args.script.println "[DEBUG] Detect the iOS identity."        
        args.script.println "[DEBUG] temp_keychain = ${temp_keychain}"

        args.script.sh """
            IOS_IDENTITY=\$(security find-identity -v -p codesigning ${temp_keychain} | head -1 | grep '"' | sed -e 's/[^"]*"//' -e 's/".*//')
            IOS_UUID=\$(security find-identity -v -p codesigning ${temp_keychain} | head -1 | grep '"' | awk '{print \$2}')
            """
    } 

    /**
     * Установка метадаты на keychain.
     *
     * @param temp_keychain             String, req - имя временного хранилища ключей.
     * @param temp_keychain_pass        String, req - пароль от временного хранилища ключей.
     */
    static void setPartitionList(Map args = [:]) {
        String temp_keychain            = args.temp_keychain
        String temp_keychain_pass       = args.temp_keychain_pass

        args.script.println "[START] Setting new requirement for MacOS 10.12"        
        args.script.println "[DEBUG] temp_keychain = ${temp_keychain}"
        args.script.println "[DEBUG] temp_keychain_pass = ${temp_keychain_pass}"
                            
        args.script.sh "security set-key-partition-list -S apple-tool:,apple: -s -k ${temp_keychain_pass} ${temp_keychain}"
    }
    
    /**
     * Удаление временного keychain.
     *
     * @param temp_keychain             String, req - имя временного хранилища ключей.
     * @param development_id            String, req - developer apple id team, в формате: Apple Development: some host (team id).
     */
    static void deleteTempKeychain(Map args = [:]) {
        String temp_keychain            = args.temp_keychain ?: 'temp_jenkins-ci.keychain'
        
        args.script.println "[START] Temp keychain will be deleted."       
        args.script.println "[DEBUG] temp_keychain = ${temp_keychain}"

        args.script.sh "security delete-certificate -c '${development_id}' ${temp_keychain}"
        args.script.sh "security delete-keychain ${temp_keychain}"
    }                 
    
    /**
     * Вывод списка всех keychain.
     */
    static void listKeychains(Map args = [:]) {    
        args.script.sh "security list-keychains"
    }            
}