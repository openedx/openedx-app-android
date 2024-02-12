//Injection to fix CSS issues for Survey xBlock
var css = `
    .survey-table:not(.poll-results) .survey-option label {
        margin-bottom: 0px !important;
    }

    .survey-table:not(.poll-results) .survey-option .visible-mobile-only {
        width: calc(100% - 21px) !important;
    }

    .survey-table:not(.poll-results) .survey-option input {
        width: 13px !important;
        height: 13px !important;
    }

    .survey-percentage .percentage {
        width: 54px !important;
    }`;
var head = document.head || document.getElementsByTagName('head')[0];
var style = document.createElement('style');

head.appendChild(style);
style.type = 'text/css';
if (style.styleSheet) {
    style.styleSheet.cssText = css;
} else {
    style.appendChild(document.createTextNode(css));
}
