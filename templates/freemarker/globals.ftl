<#assign noBeatmapIcon = "/img/nobeatmapicon.png"> 
<#function convertStatusBack status>
    <#switch status>
        <#case 0><#return "Not submitted"><#break>
        <#case 1><#return "Pending"><#break>
        <#case 2><#return "Ranked"><#break>
        <#case 3><#return "Approved"><#break>
        <#case 4><#return "Qualified"><#break>
        <#case 5><#return "Loved"><#break>
        <#default><#return "Unknown"><#break>
    </#switch>
</#function>

<#function convertScoreStatus status>
    <#switch status>
        <#case 0><#return "Not submitted"><#break>
        <#case 2><#return "Submitted"><#break>
        <#default><#return "Unknown"><#break>
    </#switch>
</#function>


<#function convertStatusBackColor color>
    <#switch color>
        <#case 0><#return "success"><#break>
        <#case 1><#return "success"><#break>
        <#case 2><#return "success"><#break>
        <#case 3><#return "info"><#break>
        <#case 4><#return "warning"><#break>
        <#case 5><#return "danger"><#break>
        <#default><#return "success"><#break>
    </#switch>
</#function>

<#function convertDiffToColor diff>
    <#if diff?contains("Easy")>
        <#return "success">
    <#elseif diff?contains("Normal")>
        <#return "info">
    <#elseif diff?contains("Hard")>
        <#return "warning">
    <#elseif diff?contains("Insane")>
        <#return "danger">
    <#else>
        <#return "reset">
    </#if>
</#function>

<#function convertModeBack mode>
    <#switch mode>
        <#case 0><#return "osu"><#break>
        <#case 4><#return "OSURX"><#break>
        <#case 8><#return "OSUAP"><#break>
        <#case 1><#return "taiko"><#break>
        <#case 2><#return "catch"><#break>
        <#case 3><#return "mania"><#break>
        <#case 5><#return "TAIKORX"><#break>
        <#case 6><#return "CATCHRX"><#break>
        <#default><#return "Unknown"><#break>
    </#switch>
</#function>

<#function convertModeToNumber modeString>
    <#switch modeString?upper_case>
        <#case "OSU"><#return 0><#break>
        <#case "OSURX"><#return 4><#break>
        <#case "OSUAP"><#return 8><#break>
        <#case "TAIKO"><#return 1><#break>
        <#case "CATCH"><#return 2><#break>
        <#case "MANIA"><#return 3><#break>
        <#case "TAIKORX"><#return 5><#break>
        <#case "CATCHRX"><#return 6><#break>
        <#default><#return -1><#break>
    </#switch>
</#function>


<#function convertModeIcon toIcon>
    <#switch mode>
        <#case 1><#return "/img/modes/taiko.svg"><#break>
        <#case 2><#return "/img/modes/catch.svg"><#break>
        <#case 3><#return "/img/modes/mania.svg"><#break>
        <#default><#return "/img/modes/osu.svg"><#break>
    </#switch>
</#function>

<#function convertPlaytime totalSeconds>
    <#assign days = (totalSeconds / (24 * 60 * 60))?int /> <#-- Total seconds in a day -->
    <#assign hours = ((totalSeconds % (24 * 60 * 60)) / (60 * 60))?int /> <#-- Total seconds in an hour -->
    <#assign minutes = ((totalSeconds % (60 * 60)) / 60)?int /> <#-- Total seconds in a minute -->

    <#local result = {} />
    
    <#if days gt 0>
        <#local result = result + {"days": days} />
    </#if>
    
    <#if hours gt 0>
        <#local result = result + {"hours": hours} />
    </#if>
    
    <#if minutes gt 0>
        <#local result = result + {"minutes": minutes} />
    </#if>
    
    <#return result />
</#function>
<#function convertModeToRelax(mode)>
    <#switch mode>
        <#case 0><#return 4><#break>
        <#case 1><#return 5><#break>
        <#case 2><#return 6><#break>
        <#case 8><#return 4><#break>
    </#switch>
</#function>