<!DOCTYPE html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
</head>
<body>
Hello!
<p>
    You have one alert generated with below details.
    <#if alert.labels?has_content>
    <p>Labels: <br/>
        <ul>
        <#list alert.labels?keys as prop>
            <#if prop?has_content>
            <li>${prop} = ${alert.labels[prop]!""}</li>
            </#if>
        </#list>
        </ul>
    </p>
    </#if>
    <#if alert.annotations?has_content>
    <p>
        Annotations: <br/>
        <ul>
        <#list alert.annotations?keys as prop>
            <#if prop?has_content>
            <li>${prop} = ${alert.annotations[prop]!""}</li>
            </#if>
        </#list>
        </ul>
    </p>
    </#if>
    <#if alert.expectedValue?has_content>
    <p>Expected Value: ${alert.expectedValue}</p>
    </#if>
    <#if alert.observedValue?has_content>
    <p>Observed Value: ${alert.observedValue}</p>
    </#if>
</p>

<#if alert.generatorURL?has_content>
<p>More details on alert can be accessed from <a href="${alert.generatorURL}">here</a></p>
</#if>

<p>You can access alert subscription rules from the <a href="">console</a>.</p>

<p>
    Regards, <br />
    <em>Alert Manager Team</em>
</p>
</body>
</html>
