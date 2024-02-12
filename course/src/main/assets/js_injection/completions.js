//Injection to intercept completion state for xBlocks
$(document).on("ajaxSuccess", function(event, request, settings) {
    console.log("loaded url is = " + settings.url);
    if (settings.url.includes("publish_completion") &&
        request.responseText.includes("ok")) {
        javascript:window.callback.completionSet();
    }
});
