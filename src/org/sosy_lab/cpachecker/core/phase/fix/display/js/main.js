window.onload = initAll;

var _ace_editor;
// global variable stores which file is opened by editor
var _current_file;
// the tab size for the editor
var _tab_size = 4;
// historic markers are stored for removal
var _marker_set = [];
// we use range to mark code in the editor
var Range = ace.require('ace/range').Range;
// the flag shows whether page close-down is normal
var _normal_exit = false;

function initAll() {
    initEditor();
    initFileTree();
}

function initFileTree() {
    $('#file_tree').fileTree({ root: 'fileTree', script: 'http://localhost:9026', expandSpeed: 1, collapseSpeed:1 }, function(file) {
        loadFile(file);
    });
}

function initEditor() {
    _ace_editor = ace.edit("editor");
    _ace_editor.renderer.setHScrollBarAlwaysVisible(false);
    _ace_editor.renderer.setShowGutter(true);
    _ace_editor.setTheme("ace/theme/xcode");
    _ace_editor.setShowPrintMargin(true);
    _ace_editor.setDisplayIndentGuides(true);
    _ace_editor.setHighlightSelectedWord(true);
    _ace_editor.setHighlightActiveLine(false);
    _ace_editor.setPrintMarginColumn(80);
    _ace_editor.setAnimatedScroll(true);
    // the editor should be read-only
    _ace_editor.setReadOnly(true);

    _ace_editor.session.setMode("ace/mode/c_cpp");
    _ace_editor.session.setValue("Choose a file to show its source.");
    _ace_editor.session.setTabSize(_tab_size);
    _ace_editor.session.setUseWrapMode(false);
}

function loadFile(file) {
    _ace_editor.session.clearAnnotations();
    if (file === _current_file) {
        return;
    }
    // remove the existing markers
    removeMarker();
    // remove existing fix details
    removeDetail();
    // cache the selected fixes for this file
    var mode_text = $('#mode_selected').text();
    if (typeof _current_file !== 'undefined' && mode_text === 'Manual') {
        var selected = [];
        $('.circular.button.active').parent().parent().each(function () {
            selected.push(this.id);
        });
        $.post("http://localhost:9026", { op: 'cache', file: file, list: String(selected) });
    }
    // load source code
    $.get("http://localhost:9026", { file: file }, function(content) {
       _current_file = file;
       _ace_editor.session.setValue(unescape(content));
    });
    // load fix list
    $.post("http://localhost:9026", { dir: 'fixList', file: file, mode: mode_text }).done(function (content) {
        var l = $('#fix_list');
        l.find('*').remove();
        l.append(content);
        $('.item.intfix').click(selectFix);
        $('.circular.ui.button').on('click', function(event) {
            event.stopPropagation();
            var item, indent, new_indent, button;
            if ($(this).hasClass('active') && $('#mode_selected').text() === 'Manual') {
                $(this).removeClass('active');
                // we also remove the active status of depending fixes
                item = $(this).parent().parent();
                indent = Number($(item).attr("data-indent"));
                if (indent > 0) {
                    item = $(item).prev();
                    while ($(item).hasClass("item intfix")) {
                        new_indent = Number($(item).attr("data-indent"));
                        if (new_indent < indent) {
                            indent = new_indent;
                            button = $(item).children().children('button');
                            $(button).removeClass('active');
                        }
                        if (indent === 0) {
                            break;
                        }
                        item = $(item).prev();
                    }
                }
            } else {
                $(this).addClass('active');
                // we also set the depended fixes as active
                item = $(this).parent().parent();
                indent = Number($(item).attr("data-indent"));
                item = $(item).next();
                while ($(item).hasClass("item intfix")) {
                    new_indent = Number($(item).attr("data-indent"));
                    if (new_indent > indent) {
                        button = $(item).children().children('button');
                        $(button).addClass('active');
                    } else {
                        break;
                    }
                    item = $(item).next();
                }
            }
        });
    });
}

function selectFix() {
    var fix_id = $(this).attr('id');
    $.post("http://localhost:9026", { dir: 'fixDraw', id: fix_id }).done(function (content) {
        // the response is in JSON format
        var info_json = JSON.parse(content);
        var start_line = Number(info_json.startLine);
        var end_line = Number(info_json.endLine);
        var start_offset = Number(info_json.startOffset);
        var end_offset = Number(info_json.endOffset);
        // remove old ranges before drawing the new range
        removeMarker();
        // draw ranges in the editor
        var marker = _ace_editor.session.addMarker(new Range(start_line - 1, start_offset, end_line - 1, end_offset), "ace-code-highlight", "line", false);
        _ace_editor.scrollToLine(start_line - 1, true, true);
        _marker_set.push(marker);
        // initialize fix detail panel
        removeDetail();
        // parse fix detail into readable content
        var fix_mode = info_json.mode;
        var fix_type = codeWrap(info_json.type);
        var detail_text = '';
        switch (fix_mode) {
            case 'SPECIFIER': {
                var var_name = codeWrap(info_json._var);
                detail_text = detail_text + "IntPTI advices you to change the declared tye of " + var_name +
                    " to " + fix_type + " which is consistent with its usage context.";
                break;
            }
            case 'CHECK_ARITH': {
                var optr = info_json._optr;
                var is_signed = Boolean(Number(info_json._sign));
                var op1 = codeWrap(info_json._op1);
                var op2 = codeWrap(info_json._op2);
                detail_text = detail_text + "IntPTI advices you to add a sanity check to examine if the certain expression has overflow issue. " +
                    "This is because the " + optr + " of " + op1 + " and " + op2 + " possibly has " + (is_signed ? "a signed" : "a unsigned") +
                        " overflow error while it is on the critical program site.";
                break;
            }
            case 'CHECK_CONV': {
                var exp = codeWrap(info_json._exp);
                var origin_type = codeWrap(info_json._origin);
                var demand_type = codeWrap(info_json._demand);
                detail_text = detail_text + "IntPTI advices you to add an sanity check on " + exp + " to examine if its value " +
                    "could be represented in " + demand_type + ". This is because " + exp + " is converted from " + origin_type +
                    " to " + demand_type + " forcibly and it has possible representation issue.";
                break;
            }
            default: {
                // then the fix mode should be CAST
                var defect_kind = info_json._defect;
                if (defect_kind === 'conversion') {
                    var old_type = codeWrap(info_json._origin);
                    var new_type = codeWrap(info_json._target);
                    var exp0 = codeWrap(info_json._op);
                    detail_text = detail_text + "IntPTI advices you to introduce an explicit cast on " + exp0 + " to " +
                            fix_type + " because " + exp0 + " is to be converted from " + old_type + " to " + new_type +
                        " which is potentially unexpected.";
                } else {
                    var ary = Number(info_json._ary);
                    var optr0 = '<code class="inline-optr">' + info_json._optr + '</code>';
                    var signed = Boolean(Number(info_json._sign));
                    if (ary === 2) {
                        var op21 = codeWrap(info_json._op1);
                        var op22 = codeWrap(info_json._op2);
                        detail_text = detail_text + "IntPTI advices you to introduce an explicit cast on certain expression " +
                                "to " + fix_type + " because there is a possible " + (signed ? "signed" : "unsigned") +
                            " overflow on the " + op21 + optr0 + op22 + ".";
                    } else {
                        // ary === 1
                        var op11 = codeWrap(info_json._op);
                        detail_text = detail_text + "IntPTI advices you to introduce an explicit cast on certain expression " +
                                "to " + fix_type + " because there is a possible " + (signed ? "signed" : "unsigned") +
                            " overflow on the " + optr0 + op11 + ".";
                    }
                }
            }
        }
        $('#reason_text').append(detail_text);
    });
}

function codeWrap(code) {
    return '<code class="inline-code">' + code + '</code>';
}

function removeMarker() {
    for (var i in _marker_set) {
        _ace_editor.session.removeMarker(_marker_set[i]);
    }
    _marker_set = [];
}

function removeDetail() {
    $('#reason_text').html('');
}

function endSession() {
    var chosen_mode = $('#mode_selected').text();
    if (chosen_mode === 'Mode') {
        $('#end_prompt').modal({
            onDeny: function () {
                return true;
            },
            onApprove: function () {
                endSession0(chosen_mode);
            }
        }).modal('show');
        return;
    }
    endSession0(chosen_mode);
}

function endSession0(chosen_mode) {
    _normal_exit = true;
    if (chosen_mode === 'Manual') {
        // cache the current selected fixes in the list
        if (typeof _current_file !== 'undefined') {
            var selected = [];
            $('.circular.button.active').parent().parent().each(function () {
                selected.push(this.id);
            });
            $.post("http://localhost:9026", { op: 'cache', file: _current_file, list: String(selected) });
        }
    }
    // end the server session and then close the browser window
    $.post("http://localhost:9026", { op: 'close', mode: chosen_mode }).done(function () {
        window.close();
    });
}

window.onbeforeunload = function (e) {
    if (!_normal_exit) {
        var dialogText = 'Are you sure to exit the page?\n' +
            'Direct exit is equivalent to click the "proceed" button.';
        e.returnValue = dialogText;
        return dialogText;
    }
    // otherwise, nothing is done
    return null;
};

window.onunload = function () {
    if (!_normal_exit) {
        endSession0($('#mode_selected').text());
    }
};