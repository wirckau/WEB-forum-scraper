<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${pageTitle!"Forum"}</title>
    <link rel="stylesheet" href="style.css">
</head>

<body>
    <#if ulThreads??>
        <#list ulThreads as ulThread>
            <ul>${ulThread_index + 1} ${ulThread.title} </ul>
        </#list>
    </#if>

    <#if threads??>
        <#list threads as thread>
            <hr><br>
            <div id="thread"><a id=${thread.url}><a href=${thread.url}>${thread.title}</a></a></div>
            <#if posts??>
                <#list posts as post>
                    <div id="container">
                        <section id="author">
                            <p>${post.author}</p>${post.date}
                        </section>
                        <section id="content">
                            <br>
                            ${post.comment}
                        </section>
                    </div>
                </#list>
            </#if>
        </#list>
    </#if>
    <p>${error!}</p>
    <#if nxtThreads??>
        <#list nxtThreads as nxtThread>
            <div id="next">
                <a href=&num;${nxtThread.url}>Skip to the NEXT</a>
            </div>
        </#list>
    </#if>

</body>

</html>