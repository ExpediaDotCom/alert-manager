Hello!

You have one alert generated with below details.

Labels:

<#list alert.labels?keys as prop>
   ${prop} = ${alert.labels[prop]}
</#list>
<#if alert.annotations?has_content>

Annotations:
<#list alert.annotations?keys as prop>
   ${prop} = ${alert.annotations[prop]}
</#list>
</#if>

<#if alert.expectedValue?has_content>

Expected Value: ${alert.expectedValue}
</#if>
<#if alert.observedValue?has_content>
Observed Value: ${alert.observedValue}
</#if>

<#if alert.generatorURL?has_content>
More details on alert can be accessed from <${alert.generatorURL}|here>
</#if>

You can access alert subscription rules from the <http://console|console>.
