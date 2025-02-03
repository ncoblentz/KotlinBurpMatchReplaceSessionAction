import burp.api.montoya.BurpExtension
import burp.api.montoya.MontoyaApi
import burp.api.montoya.http.sessions.ActionResult
import burp.api.montoya.http.sessions.SessionHandlingAction
import burp.api.montoya.http.sessions.SessionHandlingActionData
import com.nickcoblentz.montoya.LogLevel
import com.nickcoblentz.montoya.MontoyaLogger
import com.nickcoblentz.montoya.settings.ExtensionSettingSaveLocation
import com.nickcoblentz.montoya.settings.ExtensionSettingsUnloadHandler
import com.nickcoblentz.montoya.settings.GenericExtensionSettingsFormGenerator
import com.nickcoblentz.montoya.settings.StringExtensionSetting
import com.nickcoblentz.montoya.settings.ExtensionSettingsContextMenuProvider
import burp.api.montoya.http.message.requests.HttpRequest
import kotlin.text.Regex


class MatchReplaceSessionExtension : BurpExtension, SessionHandlingAction {
    private lateinit var api: MontoyaApi


    private lateinit var logger: MontoyaLogger

    private val pluginName = "Match/Replace Session"
    private val regexOptions = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE)

    private lateinit var matchOneSetting : StringExtensionSetting
    private lateinit var replaceOneSetting : StringExtensionSetting
    private lateinit var matchTwoSetting : StringExtensionSetting
    private lateinit var replaceTwoSetting : StringExtensionSetting
    private lateinit var matchThreeSetting : StringExtensionSetting
    private lateinit var replaceThreeSetting : StringExtensionSetting
    private lateinit var testInputSetting : StringExtensionSetting


    override fun initialize(api: MontoyaApi?) {

        // In Kotlin, you have to explicitly define variables as nullable with a ? as in MontoyaApi? above
        // This is necessary because the Java Library allows null to be passed into this function
        // requireNotNull is a built-in Kotlin function to check for null that throws an Illegal Argument exception if it is null
        // after checking for null, the Kotlin compiler knows that any reference to api  or this.api below will not = null and you no longer have to check it
        // Finally, assign the MontoyaApi instance (not nullable) to a class property to be accessible from other functions in this class
        this.api = requireNotNull(api) { "api : MontoyaApi is not allowed to be null" }
        // This will print to Burp Suite's Extension output and can be used to debug whether the extension loaded properly

        logger = MontoyaLogger(api, LogLevel.DEBUG)
        logger.debugLog("Started loading the extension...")

        matchOneSetting = StringExtensionSetting(
            // pass the montoya API to the setting
            api,
            // Give the setting a name which will show up in the Swing UI Form
            "First Match Regex Expression",
            // Key for where to save this setting in Burp's persistence store
            "MatchReplace.MatchOne",
            // Default value within the Swing UI Form
            "",
            // Whether to save it for this specific "PROJECT" or as a global Burp "PREFERENCE"
            ExtensionSettingSaveLocation.PROJECT
            )

        replaceOneSetting = StringExtensionSetting(
            // pass the montoya API to the setting
            api,
            // Give the setting a name which will show up in the Swing UI Form
            "First Replace Expression",
            // Key for where to save this setting in Burp's persistence store
            "MatchReplace.ReplaceOne",
            // Default value within the Swing UI Form
            "",
            // Whether to save it for this specific "PROJECT" or as a global Burp "PREFERENCE"
            ExtensionSettingSaveLocation.PROJECT
        )

        matchTwoSetting = StringExtensionSetting(
            // pass the montoya API to the setting
            api,
            // Give the setting a name which will show up in the Swing UI Form
            "Second Match Regex Expression",
            // Key for where to save this setting in Burp's persistence store
            "MatchReplace.MatchTwo",
            // Default value within the Swing UI Form
            "",
            // Whether to save it for this specific "PROJECT" or as a global Burp "PREFERENCE"
            ExtensionSettingSaveLocation.PROJECT
        )

        replaceTwoSetting = StringExtensionSetting(
            // pass the montoya API to the setting
            api,
            // Give the setting a name which will show up in the Swing UI Form
            "Second Replace Expression",
            // Key for where to save this setting in Burp's persistence store
            "MatchReplace.ReplaceTwo",
            // Default value within the Swing UI Form
            "",
            // Whether to save it for this specific "PROJECT" or as a global Burp "PREFERENCE"
            ExtensionSettingSaveLocation.PROJECT
        )

        matchThreeSetting = StringExtensionSetting(
            // pass the montoya API to the setting
            api,
            // Give the setting a name which will show up in the Swing UI Form
            "Third Match Regex Expression",
            // Key for where to save this setting in Burp's persistence store
            "MatchReplace.MatchThree",
            // Default value within the Swing UI Form
            "",
            // Whether to save it for this specific "PROJECT" or as a global Burp "PREFERENCE"
            ExtensionSettingSaveLocation.PROJECT
        )

        replaceThreeSetting = StringExtensionSetting(
            // pass the montoya API to the setting
            api,
            // Give the setting a name which will show up in the Swing UI Form
            "Third Replace Expression",
            // Key for where to save this setting in Burp's persistence store
            "MatchReplace.ReplaceThree",
            // Default value within the Swing UI Form
            "",
            // Whether to save it for this specific "PROJECT" or as a global Burp "PREFERENCE"
            ExtensionSettingSaveLocation.PROJECT
        )

        testInputSetting = StringExtensionSetting(
            // pass the montoya API to the setting
            api,
            // Give the setting a name which will show up in the Swing UI Form
            "Test Input Box",
            // Key for where to save this setting in Burp's persistence store
            "MatchReplace.input",
            // Default value within the Swing UI Form
            "",
            // Whether to save it for this specific "PROJECT" or as a global Burp "PREFERENCE"
            ExtensionSettingSaveLocation.PROJECT
        )



        // Create a list of all the settings defined above
        // Don't forget to add more settings here if you define them above
        val extensionSetting = listOf(matchOneSetting,replaceOneSetting,matchTwoSetting,replaceTwoSetting,matchThreeSetting,replaceThreeSetting,testInputSetting)

        val gen = GenericExtensionSettingsFormGenerator(extensionSetting, pluginName)
        val settingsFormBuilder = gen.getSettingsFormBuilder()
        settingsFormBuilder.startRow().addTextArea("Test Match/Replace Output").setID("_output").setDisabled().endRow()
        gen.addSaveCallback { formElement, form -> form.getById("_output").value = wrapText(doMatchReplace(testInputSetting.currentValue)) }
        val settingsForm = settingsFormBuilder.run()

        // Tell Burp we want a right mouse click context menu for accessing the settings
        api.userInterface().registerContextMenuItemsProvider(ExtensionSettingsContextMenuProvider(api, settingsForm))

        // When we unload this extension, include a callback that closes any Swing UI forms instead of just leaving them still open
        api.extension().registerUnloadingHandler(ExtensionSettingsUnloadHandler(settingsForm))


        api.http().registerSessionHandlingAction(this);

        // Name our extension when it is displayed inside of Burp Suite
        api.extension().setName(pluginName)


        // See logging comment above
        logger.debugLog("...Finished loading the extension")

    }

    override fun name() = pluginName

    override fun performAction(actionData: SessionHandlingActionData?): ActionResult? {
        actionData?.request()?.let {

            return ActionResult.actionResult(HttpRequest.httpRequest(it.httpService(),doMatchReplace(it.toString())))
        }
        return ActionResult.actionResult(null)
    }

    private fun doMatchReplace(requestInputString: String): String {

        logger.debugLog("Found:\n$requestInputString")
        logger.debugLog("Match: ${matchOneSetting.currentValue}")
        logger.debugLog("Replace: ${replaceOneSetting.currentValue}")


        var newString = Regex(matchOneSetting.currentValue, regexOptions).replace(requestInputString,replaceOneSetting.currentValue)
        newString = Regex(matchTwoSetting.currentValue, regexOptions).replace(newString,replaceTwoSetting.currentValue)
        newString = Regex(matchThreeSetting.currentValue, regexOptions).replace(newString,replaceThreeSetting.currentValue)

        logger.debugLog("Result: $newString")
        return newString

    }

    private fun wrapText(text: String, width: Int = 120): String {
        val regex = Regex("(.{1,$width})(?:\\s+|\\$\\n?)|(.{1,$width})")
        return text.replace(regex, "$1$2\n")
    }


}

private fun wrapText(text: String, width: Int = 120): String {
    val regex = Regex("(.{1,$width})(?:\\s+|\\$\\n?)|(.{1,$width})")
    return text.replace(regex, "$1$2\n")
}

fun main() {
    val input = "POST /SymXchange/accountspayable HTTP/1.1 Accept-Encoding: gzip, deflate, br Content-Type: text/xml;charset=UTF-8 SOAPAction: \"http://www.symxchange.generated.symitar.com/crud/accountspayable/searchInvoiceNotePagedSelectFields\" Content-Length: 4122 Host: smopds2p48.jhacorp.com:64188 User-Agent: Apache-HttpClient/4.5.5 (Java/17.0.12) Connection: keep-alive  <soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:acc=\"http://www.symxchange.generated.symitar.com/crud/accountspayable\" xmlns:tns=\"http://www.symxchange.generated.symitar.com/symxcommon\">    <soapenv:Header/>    <soapenv:Body>       <acc:searchInvoiceNotePagedSelectFields>          <!--Optional:-->          <Request MessageId=\"?\" acc:BranchId=\"?\">             <!--Optional:-->             <VendorNumber>1</VendorNumber>             <!--Optional:-->             <InvoiceLocator>1</InvoiceLocator>             <!--Optional:-->             <Credentials tns:ProcessorUser=\"?\">                <!--You have a CHOICE of the next 3 items at this level-->                <!--Optional:-->                <AdministrativeCredentials>                   <!--Optional:-->                   <Password>?</Password>                   <Version_1/>                   <!--You may enter ANY elements at this point-->                </AdministrativeCredentials>                <!--Optional:-->                <UserNumberCredentials>                   <!--Optional:-->                   <UserNumber>?</UserNumber>                   <!--Optional:-->                   <Password>?</Password>                   <Version_1/>                   <!--You may enter ANY elements at this point-->                </UserNumberCredentials>                <!--Optional:-->                <TokenCredentials>                   <!--Optional:-->                   <TokenId>?</TokenId>                   <Version_1/>                   <!--You may enter ANY elements at this point-->                </TokenCredentials>             </Credentials>             <!--Optional:-->             <DeviceInformation DeviceType=\"?\" DeviceNumber=\"?\"/>             <!--Optional:-->             <PagingRequestContext>                <!--Optional:-->                <NumberOfRecordsToReturn>10</NumberOfRecordsToReturn>                <!--Optional:-->                <NumberOfRecordsToSkip>0</NumberOfRecordsToSkip>                <!--Optional:-->                <Token>?</Token>                <Version_1/>                <!--You may enter ANY elements at this point-->             </PagingRequestContext>             <!--Optional:-->             <SelectableFields>                <!--Optional:-->                <IncludeAllInvoiceNoteFields>true</IncludeAllInvoiceNoteFields>                <!--Optional:-->                <InvoiceNoteFields>                   <!--Optional:-->                   <Code>?</Code>                   <!--Optional:-->                   <EnterDate>?</EnterDate>                   <!--Optional:-->                   <EnterTime>?</EnterTime>                   <!--Optional:-->                   <ExpirationDate>?</ExpirationDate>                   <!--Optional:-->                   <Id>?</Id>                   <!--Optional:-->                   <IdType>?</IdType>                   <!--Optional:-->                   <Locator>?</Locator>                   <!--Optional:-->                   <RecordChangeDate>?</RecordChangeDate>                   <!--0 to 50 repetitions:-->                   <Text>?</Text>                   <!--Optional:-->                   <UserId>?</UserId>                   <!--Optional:-->                   <VoidFlag>?</VoidFlag>                   <!--Optional:-->                   <VoidedBy>?</VoidedBy>                   <Version_1/>                   <!--You may enter ANY elements at this point-->                </InvoiceNoteFields>                <Version_1/>                <!--You may enter ANY elements at this point-->             </SelectableFields>             <!--Optional:-->             <SearchFilter>                <!--Optional:-->                <Query>?</Query>                <Version_1/>                <!--You may enter ANY elements at this point-->             </SearchFilter>             <Version_1/>             <!--You may enter ANY elements at this point-->          </Request>       </acc:searchInvoiceNotePagedSelectFields>    </soapenv:Body> </soapenv:Envelope>";

    println(wrapText(Regex("<DeviceInformation DeviceType=\".*\" DeviceNumber=\".*\"/>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE))
        .replace(input,"<DeviceInformation DeviceType=\"symxdevice\" DeviceNumber=\"0\"/>")))
}