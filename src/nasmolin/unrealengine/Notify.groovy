package lab.nasmolin.unrealengine

class Notify implements Serializable {
    
    /**
     *
     * Discord notifier 
     * Функция отправки сообщений в дискорд.
     * 
     * Для составления тела сообщения необходимы глобальные переменные. 
     * Используем static метод, поэтому передаем их через аргументы.
     * 
     * static method:
     * @param discordApi        String, opt - адрес Api (default: https://discord.com/api/webhooks/)
     * @param discordWebhook    String, opt - без адреса Api(задается отдельно в dicordApi)
     * @param discordMessage    String, opt - содержание сообщения 
     * @param script            Script, req - Контекст DSL
     * @param currentResult     String, opt - статус сборки, default: UNSTABLE
     *  
     * from global env:
     * @param BUILD_URL         String, opt
     * @param nexusRepo         String, opt
     * @param nexusRepoUrl      String, opt
     * @param artifactUrl       String, opt
     * @param projectName       String, opt
     * 
     */
    static void discord(Map args = [:]) {
        
        String BUILD_URL = args.BUILD_URL ?: "https://jenkins.domain.com/view/all/builds"
        String nexusRepo = args.nexusRepo ?: "Jenkins"
        String nexusRepoUrl = args.nexusRepoUrl ?: "https://nexus.domain.com/#browse/browse"
        String artifactUrl = args.artifactUrl ?: "https://nexus.domain.com/#browse/browse"
        String projectName = args.projectName ?: "MyProjectName"
        String discordApi = args.discordApi ?: "https://discord.com/api/webhooks/"
        String discordWebhook = args.discordWebhook ?: "webhook!"
        String currentResult = args.currentResult ?: "UNSTABLE"
        String discordMessage = args.discordMessage ?: 
            """**Links:** 
            > * [Jenkins: Job](${BUILD_URL})
            > * [Jenkins: Job Console Output](${BUILD_URL}/console)
            > * [Nexus: ${nexusRepo} Repository](${nexusRepoUrl})
            > * [Nexus: Artifact](${artifactUrl})
            """

        args.script.discordSend(
            showChangeset:true,
            enableArtifactsList:true,
            description:discordMessage,
            footer:"Build status: " + currentResult,
            result:currentResult,
            title:projectName + ": ${nexusRepo} build",
            webhookURL:discordApi+discordWebhook)
    }
}