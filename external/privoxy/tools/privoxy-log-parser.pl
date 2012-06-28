#!/usr/bin/perl

################################################################################
# privoxy-log-parser
#
# A parser for Privoxy log messages. For incomplete documentation run
# perldoc privoxy-log-parser(.pl), for fancy screenshots see:
#
# http://www.fabiankeil.de/sourcecode/privoxy-log-parser/
#
# $Id: privoxy-log-parser.pl,v 1.124 2011/11/13 16:54:10 fabiankeil Exp $
#
# TODO:
#       - LOG_LEVEL_CGI, LOG_LEVEL_ERROR, LOG_LEVEL_WRITE content highlighting
#       - create fancy statistics
#       - grep through Privoxy sources to find unsupported log messages
#       - hunt down substitutions that match content from variables which
#         can contain stuff like ()?'[]
#       - replace $h{'foo'} with h('foo') where possible
#       - hunt down XXX comments instead of just creating them
#       - add example log lines for every regex and mark them up for
#         regression testing
#       - Handle incomplete input without Perl warning about undefined variables.
#       - Use generic highlighting function that takes a regex and the
#         hash key as input.
#       - Add --compress and --decompress options.
#
# Copyright (c) 2007-2010 Fabian Keil <fk@fabiankeil.de>
#
# Permission to use, copy, modify, and distribute this software for any
# purpose with or without fee is hereby granted, provided that the above
# copyright notice and this permission notice appear in all copies.
#
# THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
# WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
# MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
# ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
# WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
# ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
# OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
################################################################################

use strict;
use warnings;
use Getopt::Long;

use constant {
    PRIVOXY_LOG_PARSER_VERSION => '0.7',
    # Feel free to mess with these ...
    DEFAULT_BACKGROUND => 'black',  # Choose registered colour (like 'black')
    DEFAULT_TEXT_COLOUR => 'white', # Choose registered colour (like 'black')
    HEADER_DEFAULT_COLOUR => 'yellow',
    REGISTER_HEADERS_WITH_THE_SAME_COLOUR => 1,

    CLI_OPTION_DEFAULT_TO_HTML_OUTPUT => 0,
    CLI_OPTION_TITLE => 'Privoxy-Log-Parser in da house',
    CLI_OPTION_NO_EMBEDDED_CSS => 0,
    CLI_OPTION_NO_MSECS => 0,
    CLI_OPTION_NO_SYNTAX_HIGHLIGHTING => 0,
    CLI_OPTION_SHORTEN_THREAD_IDS => 0,
    CLI_OPTION_SHOW_INEFFECTIVE_FILTERS => 0,
    CLI_OPTION_ACCEPT_UNKNOWN_MESSAGES => 0,
    CLI_OPTION_STATISTICS => 0,
    CLI_OPTION_UNBREAK_LINES_ONLY => 0,
    CLI_OPTION_URL_STATISTICS_THRESHOLD => 0,
    CLI_OPTION_HOST_STATISTICS_THRESHOLD => 0,

    SUPPRESS_SUCCEEDED_FILTER_ADDITIONS => 1,
    SHOW_SCAN_INTRO => 0,
    SHOW_FILTER_READIN_IN => 0,
    SUPPRESS_EMPTY_LINES => 1,
    SUPPRESS_SUCCESSFUL_CONNECTIONS => 1,
    SUPPRESS_ACCEPTED_CONNECTIONS => 1,
    SUPPRESS_GIF_NOT_CHANGED => 1,
    SUPPRESS_NEED_TO_DE_CHUNK_FIRST => 1,

    DEBUG_HEADER_REGISTERING => 0,
    DEBUG_HEADER_HIGHLIGHTING => 0,
    DEBUG_TICKS => 0,
    DEBUG_PAINT_IT => 0,
    DEBUG_SUPPRESS_LOG_MESSAGES => 0,

    PUNISH_MISSING_LOG_KNOWLEDGE_WITH_DEATH => 0,
    PUNISH_MISSING_HIGHLIGHT_KNOWLEDGE_WITH_DEATH => 1,

    LOG_UNPARSED_LINES_TO_EXTRA_FILE => 0,
    ERROR_LOG_FILE => '/var/log/privoxy-log-parser',

    # You better leave these alone unless you know what you're doing.
    COLOUR_RESET      => "\033[0;0m",
    ESCAPE => "\033[",
};

# For performance reasons, these are global.

my $t;
my %req; # request data from previous lines
my %h;
my %thread_colours;
my @all_colours;
my @time_colours;
my $thread_colour_index = 0;
my $header_colour_index = 0;
my $time_colour_index = 0;
my %header_colours;
my $no_special_header_highlighting;
my %reason_colours;
my %h_colours;
my $header_highlight_regex = '';

my $html_output_mode;
my $no_msecs_mode; # XXX: should probably be removed
my $shorten_thread_ids;
my $line_end;

sub prepare_our_stuff () {

    # Syntax Higlight hash
    @all_colours = (
        'red', 'green', 'brown', 'blue', 'purple', 'cyan',
        'light_gray', 'light_red', 'light_green', 'yellow',
        'light_blue', 'pink', 'light_cyan', 'white'
    );

    %h = (
        # LOG_LEVEL
        Info            => 'blue',
        Header          => 'green',
        Filter          => 'purple', # XXX: Used?
        'Re-Filter'     => 'purple',
        Connect         => 'brown',
        Request         => 'light_cyan',
        CGI             => 'light_green',
        Redirect        => 'cyan',
        Error           => 'light_red',
        Crunch          => 'cyan',
        'Fatal error'   => 'light_red',
        'Gif-Deanimate' => 'blue',
        Force           => 'red',
        Writing         => 'light_green',
        Received        => 'yellow',
        # ----------------------
        URL                  => 'yellow',
        path                 => 'brown',
        request_             => 'brown', # host+path but no protocol
        'ip-address'         => 'yellow',
        Number               => 'yellow',
        Standard             => 'reset',
        Truncation           => 'light_red',
        Status               => 'brown',
        Timestamp            => 'brown',
        Crunching            => 'light_red',
        crunched             => 'light_red',
        'Request-Line'       => 'pink',
        method               => 'purple',
        destination          => 'yellow',
        'http-version'       => 'pink',
        'crunch-pattern'     => 'pink',
        not                  => 'brown',
        file                 => 'brown',
        signal               => 'yellow',
        version              => 'green',
        'program-name'       => 'cyan',
        port                 => 'red',
        host                 => 'red',
        warning              => 'light_red',
        debug                => 'light_red',
        filter               => 'green',
        tag                  => 'green',
        tagger               => 'green',
        'status-message'     => 'light_cyan',
        'status-code'        => 'yellow',
        'invalid-request'    => 'light_red',
        'hits'               => 'yellow',
        error                => 'light_red',
        'rewritten-URL'      => 'light_red',
        'pcrs-delimiter'     => 'light_red',
        'ignored'            => 'light_red',
        'action-bits-update' => 'light_red',
        'configuration-line' => 'red',
        'content-type'       => 'yellow',
        'HOST'               => HEADER_DEFAULT_COLOUR,
    );

    %h_colours = %h;

    # Header colours need their own hash so the keys can be accessed properly
    %header_colours = (
        # Prefilled with headers that should not appear with default header colours
        Cookie => 'light_red',
        'Set-Cookie' => 'light_red',
        Warning => 'light_red',
        Default => HEADER_DEFAULT_COLOUR,
    );

    # Crunch reasons need their own hash as well
    %reason_colours = (
        'Unsupported HTTP feature'               => 'light_red',
        Blocked                                  => 'light_red',
        Untrusted                                => 'light_red',
        Redirected                               => 'green',
        'CGI Call'                               => 'white',
        'DNS failure'                            => 'red',
        'Forwarding failed'                      => 'light_red',
        'Connection failure'                     => 'light_red',
        'Out of memory (may mask other reasons)' => 'light_red',
        'No reason recorded'                     => 'light_red',
    );

    @time_colours = ('white', 'light_gray');

    # Translate highlight strings into highlight code
    prepare_highlight_hash(\%header_colours);
    prepare_highlight_hash(\%reason_colours);
    prepare_highlight_hash(\%h);
    prepare_colour_array(\@all_colours);
    prepare_colour_array(\@time_colours);
    init_css_colours();

    init_stats();
}

sub paint_it ($) {
###############################################################
# Takes a colour string and returns an ANSI escape sequence
# (unless --no-syntax-highlighting is used).
# XXX: The Rolling Stones reference has to go.
###############################################################

    my $colour = shift @_;

    return "" if cli_option_is_set('no-syntax-highlighting');

    my %light = (
        black       => 0,
        red         => 0,
        green       => 0,
        brown       => 0,
        blue        => 0,
        purple      => 0,
        cyan        => 0,
        light_gray  => 0,
        gray        => 0,
        dark_gray   => 1,
        light_red   => 1,
        light_green => 1,
        yellow      => 1,
        light_blue  => 1,
        pink        => 1,
        light_cyan  => 1,
        white       => 1,
    );

    my %text = (
        black       => 30,
        red         => 31,
        green       => 32,
        brown       => 33,
        blue        => 34,
        purple      => 35,
        cyan        => 36,
        gray        => 37,
        light_gray  => 37,
        dark_gray   => 30,
        light_red   => 31,
        light_green => 32,
        yellow      => 33,
        light_blue  => 34,
        pink        => 35,
        light_cyan  => 36,
        white       => 37,
    );

    my $bg_code = get_background();
    my $colour_code;
    our $default = default_colours();

    if (defined($text{$colour})) {
        $colour_code  = ESCAPE;
        $colour_code .= $text{$colour};
        $colour_code .= ";";
        $colour_code .= $light{$colour} ? "1" : "2";
        $colour_code .= ";";
        $colour_code .= $bg_code;
        $colour_code .= "m";
        debug_message $colour . " is \'" . $colour_code . $colour . $default . "\'" if DEBUG_PAINT_IT;

    } elsif ($colour =~ /reset/) {

        $colour_code = default_colours();

    } else {

        die "What's $colour supposed to mean?\n";
    }

    return $colour_code;
}

sub get_semantic_html_markup ($) {
###############################################################
# Takes a string and returns a span element
###############################################################

    my $type = shift @_;
    my $code;

    if ($type =~ /Standard/) {
        $code = '</span>';
    } else {
        $type = lc($type);
        $code = '<span title="' . $type . '" class="' . $type . '">';
    }

    return $code;
}

sub cli_option_is_set ($) {

    our %cli_options;
    my $cli_option = shift;

    die "Unknown CLI option: $cli_option" unless defined $cli_options{$cli_option};

    return $cli_options{$cli_option};
}

sub get_html_title () {

    our %cli_options;
    return $cli_options{'title'};

}

sub init_css_colours() {

    our %css_colours = (
        black       => "000",
        red         => "F00",
        green       => "0F0",
        brown       => "C90",
        blue        => "0F0",
        purple      => "F06", # XXX: wrong
        cyan        => "F09", # XXX: wrong
        light_gray  => "999",
        gray        => "333",
        dark_gray   => "222",
        light_red   => "F33",
        light_green => "33F",
        yellow      => "FF0",
        light_blue  => "30F",
        pink        => "F0F",
        light_cyan  => "66F",
        white       => "FFF",
    );
}

sub get_css_colour ($) {

   our %css_colours;
   my $colour = shift;

   die "What's $colour supposed to mean?\n" unless defined($css_colours{$colour});

   return '#' . $css_colours{$colour};
}

sub get_css_line ($) {

    my $class = shift;
    my $css_line;

    $css_line .= '.' . lc($class) . ' {'; # XXX: lc() shouldn't be necessary
    die "What's $class supposed to mean?\n" unless defined($h_colours{$class});
    $css_line .= 'color:' . get_css_colour($h_colours{$class}) . ';';
    $css_line .= 'background-color:' . get_css_colour(DEFAULT_BACKGROUND) . ';';
    $css_line .= '}' . "\n";

    return $css_line;
}

sub get_css_line_for_colour ($) {

    my $colour = shift;
    my $css_line;

    $css_line .= '.' . lc($colour) . ' {'; # XXX: lc() shouldn't be necessary
    $css_line .= 'color:' . get_css_colour($colour) . ';';
    $css_line .= 'background-color:' . get_css_colour(DEFAULT_BACKGROUND) . ';';
    $css_line .= '}' . "\n";

    return $css_line;
}

# XXX: Wrong solution
sub get_missing_css_lines () {

    my $css_line;

    $css_line .= '.' . 'default' . ' {';
    $css_line .= 'color:' . HEADER_DEFAULT_COLOUR . ';';
    $css_line .= 'background-color:' . get_css_colour(DEFAULT_BACKGROUND) . ';';
    $css_line .= '}' . "\n";

    return $css_line;
}

sub get_css () {

    our %css_colours; #XXX: Wrong solution

    my $css = '';

    $css .= '.privoxy-log {';
    $css .= 'color:' . get_css_colour(DEFAULT_TEXT_COLOUR) . ';';
    $css .= 'background-color:' . get_css_colour(DEFAULT_BACKGROUND) . ';';
    $css .= '}' . "\n";

    foreach my $key (keys %h_colours) {

        next if ($h_colours{$key} =~ m/reset/); #XXX: Wrong solution.
        $css .= get_css_line($key);

    }

    foreach my $colour (keys %css_colours) {

        $css .= get_css_line_for_colour($colour);

    }

    $css .= get_missing_css_lines(); #XXX: Wrong solution

    return $css;
}

sub print_intro () {

    my $intro = '';

    if (cli_option_is_set('html-output')) {

        my $title = get_html_title();

        $intro .= '<html><head>';
        $intro .= '<title>' . $title . '</title>';
        $intro .= '<style>' . get_css() . '</style>' unless cli_option_is_set('no-embedded-css');
        $intro .= '</head><body>';
        $intro .= '<h1>' . $title . '</h1><p class="privoxy-log">';

        print $intro;
    }
}

sub print_outro () {

    my $outro = '';

    if (cli_option_is_set('html-output')) {

        $outro = '</p></body></html>';
        print $outro;

    }
}

sub get_line_end () {
    return cli_option_is_set('html-output') ? "<br>\n" : "\n";
}

sub get_colour_html_markup ($) {
###############################################################
# Takes a colour string a span element. XXX: WHAT?
# XXX: This function shouldn't be necessary, the
# markup should always be semantically correct.
###############################################################

    my $type = shift @_;
    my $code;

    if ($type =~ /Standard/) {
        $code = '</span>';
    } else {
        $code = '<span class="' . lc($type) . '">';
    }

    return $code;
}

sub default_colours () {
    # XXX: Properly
    our $bg_code;
    return reset_colours();
}

sub show_colours () {
    # XXX: Implement
}

sub reset_colours () {
    return ESCAPE . "0m";
}

sub set_background ($){

    my $colour = shift;
    our $bg_code;
    my %backgrounds = (
              black       => "40",
              red         => "41",
              green       => "42",
              brown       => "43",
              blue        => "44",
              magenta     => "45",
              cyan        => "46",
              white       => "47",
              default     => "49",
    );

    if (defined($backgrounds{$colour})) {
        $bg_code = $backgrounds{$colour};
    } else {
        die "Invalid background colour: " . $colour;
    }
}

sub get_background (){
    return our $bg_code;
}

sub prepare_highlight_hash ($) {
    my $ref = shift;

    foreach my $key (keys %$ref) {
        $$ref{$key} = $html_output_mode ?
            get_semantic_html_markup($key) :
            paint_it($$ref{$key});
    }
}

sub prepare_colour_array ($) {
    my $ref = shift;

    foreach my $i (0 ... @$ref - 1) {
        $$ref[$i] = $html_output_mode ?
            get_colour_html_markup($$ref[$i]) :
            paint_it($$ref[$i]);
    }
}

sub found_unknown_content ($) {

    my $unknown = shift;
    my $message;

    return if cli_option_is_set('accept-unknown-messages');

    return if ($unknown =~ /\[too long, truncated\]$/);

    $message = "found_unknown_content: Don't know how to highlight: ";
    # Break line so the log file can later be parsed as Privoxy log file again
    $message .= '"' . $unknown . '"' . " in:\n";
    $message .= $req{$t}{'log-message'};
    debug_message($message);
    log_parse_error($req{$t}{'log-message'});

    die "Unworthy content parser" if PUNISH_MISSING_LOG_KNOWLEDGE_WITH_DEATH;
}

sub log_parse_error ($) {

    my $message = shift;

    if (LOG_UNPARSED_LINES_TO_EXTRA_FILE) {
        open(my $errorlog_fd, ">>" . ERROR_LOG_FILE) || die "Writing " . ERROR_LOG_FILE . " failed";
        print $errorlog_fd $message;
        close($errorlog_fd);
    }
}

sub debug_message (@) {
    my @message = @_;

    print $h{'debug'} . "@message" . $h{'Standard'} . "\n";
}

################################################################################
# highlighter functions that aren't loglevel-specific
################################################################################

sub h ($) {

    # Get highlight marker
    my $highlight = shift; # XXX: Stupid name;
    my $result = '';
    my $message;

    if (defined($highlight)) {

        $result = $h{$highlight};

    } else {

        $message = "h: Don't recognize highlighter $highlight.";
        debug_message($message);
        log_parser_error($message);
        die "Unworthy highlighter function" if PUNISH_MISSING_HIGHLIGHT_KNOWLEDGE_WITH_DEATH;
    }

    return $result;
}

sub highlight_known_headers ($) {

    my $content = shift;

    debug_message("Searching $content for things to highlight.") if DEBUG_HEADER_HIGHLIGHTING;

    if ($content =~ m/(?<=\s)($header_highlight_regex):/) {
        my $header = $1;
        $content =~ s@(?<=[\s|'])($header)(?=:)@$header_colours{$header}$1$h{'Standard'}@ig;
        debug_message("Highlighted '$header' in '$content'") if DEBUG_HEADER_HIGHLIGHTING;
    }

    return $content;
}

sub highlight_matched_request_line ($$) {

    my $result = shift; # XXX: Stupid name;
    my $regex = shift;
    if ($result =~ m@(.*)($regex)(.*)@) {
        $result = $1 . highlight_request_line($2) . $3
    }
    return $result;
}

sub highlight_request_line ($) {

    my $rl = shift;
    my ($method, $url, $http_version);

    #GET http://images.sourceforge.net/sfx/icon_warning.gif HTTP/1.1
    if ($rl =~ m/Invalid request/) {

        $rl = h('invalid-request') . $rl . h('Standard');

    } elsif ($rl =~ m/^([-\w]+) (.*) (HTTP\/\d\.\d)/) {

        # XXX: might not match in case of HTTP method fuzzing.
        # XXX: save these: ($method, $path, $http_version) = ($1, $2, $3);
        $rl =~ s@^(\w+)@$h{'method'}$1$h{'Standard'}@;
        if ($rl =~ /http:\/\//) {
            $rl = highlight_matched_url($rl, '[^\s]*(?=\sHTTP)');
        } else {
            $rl = highlight_matched_pattern($rl, 'request_', '[^\s]*(?=\sHTTP)');
        }

        $rl =~ s@(HTTP\/\d\.\d)$@$h{'http-version'}$1$h{'Standard'}@;

    } elsif ($rl =~ m/\.\.\. \[too long, truncated\]$/) {

        $rl =~ s@^(\w+)@$h{'method'}$1$h{'Standard'}@;
        $rl = highlight_matched_url($rl, '[^\s]*(?=\.\.\.)');

    } elsif ($rl =~ m/^ $/) {

        $rl = h('error') . "No request line specified!" . h('Standard');

    } else {

        debug_message ("Can't parse request line: $rl");

    }

    return $rl;
}

sub highlight_response_line ($) {

    my $rl = shift;
    my ($http_version, $status_code, $status_message);

    #HTTP/1.1 200 OK
    #ICY 200 OK

    # TODO: Mark different status codes differently

    if ($rl =~ m/((?:HTTP\/\d\.\d|ICY)) (\d+) (.*)/) {
        ($http_version, $status_code, $status_message) = ($1, $2, $3);
    } else {
        debug_message ("Can't parse response line: $rl") and die 'Fix this';
    }

    # Rebuild highlighted
    $rl= "";
    $rl .= h('http-version') . $http_version . h('Standard');
    $rl .= " ";
    $rl .= h('status-code') . $status_code . h('Standard');
    $rl .= " ";
    $rl .= h('status-message') . $status_message . h('Standard');

    return $rl;
}

sub highlight_matched_url ($$) {

    my $result = shift; # XXX: Stupid name;
    my $regex = shift;

    #print "Got $result, regex ($regex)\n";

    if ($result =~ m@(.*?)($regex)(.*)@) {
        $result = $1 . highlight_url($2) . $3;
        #print "Now the result is $result\n";
    }

    return $result;
}

sub highlight_matched_host ($$) {

    my ($result, $regex) = @_; # XXX: result ist stupid name;

    if ($result =~ m@(.*?)($regex)(.*)@) {
        $result = $1 . $h{host} . $2 . $h{Standard} . $3;
    }

    return $result;
}

sub highlight_matched_pattern ($$$) {

    my $result = shift; # XXX: Stupid name;
    my $key = shift;
    my $regex = shift;

    die "Unknown key $key" unless defined $h{$key};

    if ($result =~ m@(.*?)($regex)(.*)@) {
        $result = $1 . h($key) . $2 . h('Standard') . $3;
    }

    return $result;
}

sub highlight_matched_path ($$) {

    my $result = shift; # XXX: Stupid name;
    my $regex = shift;

    if ($result =~ m@(.*?)($regex)(.*)@) {
        $result = $1 . h('path') . $2 . h('Standard') . $3;
    }

    return $result;
}

sub highlight_url ($) {

    my $url = shift;

    if ($html_output_mode) {

        $url = '<a href="' . $url . '">' . $url . '</a>';

    } else {

        $url = h('URL') . $url . h('Standard');

    }

    return $url;
}

sub update_header_highlight_regex ($) {

    my $header = shift;
    my $headers = join ('|', keys %header_colours);

    $header_highlight_regex = qr/$headers/;
    print "Registering '$header'\n" if DEBUG_HEADER_HIGHLIGHTING;
}

################################################################################
# loglevel-specific highlighter functions
################################################################################

sub handle_loglevel_header ($) {

    my $c = shift;

    if ($c =~ /^scan:/) {

        if ($c =~ m/^scan: ([^: ]+):/) {

            # Register new headers
            # scan: Accept: image/png,image/*;q=0.8,*/*;q=0.5
            my $header = $1;
            if (!defined($header_colours{$header}) and $header =~ /^[\d\w-]*$/) {
                debug_message "Registering previously unknown header $1" if DEBUG_HEADER_REGISTERING;

                if (REGISTER_HEADERS_WITH_THE_SAME_COLOUR) {
                    $header_colours{$header} =  $header_colours{'Default'};
                } else {
                    $header_colours{$header} = $all_colours[$header_colour_index % @all_colours];
                    $header_colour_index++;
                }
                update_header_highlight_regex($header);
            }

        } elsif ($c =~ m/^(scan: )(\w+ .+ HTTP\/\d\.\d)/) {

            # scan: GET http://p.p/ HTTP/1.1
            $c = $1 . highlight_request_line($2);

        } elsif ($c =~ m/^(scan: )((?:HTTP\/\d\.\d|ICY) (\d+) (.*))/) {

            # scan: HTTP/1.1 200 OK
            $req{$t}{'response_line'} = $2;
            $req{$t}{'status_code'} = $3;
            $req{$t}{'status_message'} = $4;
            $c = $1 . highlight_response_line($req{$t}{'response_line'});
        }

    } elsif ($c =~ m/^Crunching (?:server|client) header: .* \(contains: ([^\)]*)\)/) {

        # Crunching server header: Set-Cookie: trac_form_token=d5308c34e16d15e9e301a456; (contains: Cookie:)
        $c =~ s@(?<=contains: )($1)@$h{'crunch-pattern'}$1$h{'Standard'}@;
        $c =~ s@(Crunching)@$h{$1}$1$h{'Standard'}@;

    } elsif ($c =~ m/^New host is: ([^\s]*)\./) {

        # New host is: trac.vidalia-project.net. Crunching Referer: http://www.vidalia-project.net/!
        $c = highlight_matched_host($c, '(?<=New host is: )[^\s]+(?=\.)');
        $c = highlight_matched_url($c, '(?<=Crunching Referer: )[^\s!]+');

    } elsif ($c =~ m/^Text mode enabled by force. (Take cover)!/) {

        # Text mode enabled by force. Take cover!
        $c =~ s@($1)@$h{'warning'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^(New HTTP Request-Line: )(.*)/) {

        # New HTTP Request-Line: GET http://www.privoxy.org/ HTTP/1.1
        $c = $1 . highlight_request_line($2);

    } elsif ($c =~ m/^Adjust(ed)? Content-Length to \d+/) {

        # Adjusted Content-Length to 2132
        # Adjust Content-Length to 33533
        $c =~ s@(?<=Content-Length to )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $c = highlight_known_headers($c);

    } elsif ($c =~ m/^Destination extracted from "Host:" header. New request URL:/) {

        # Destination extracted from "Host:" header. New request URL: http://www.cccmz.de/~ridcully/blog/
        $c = highlight_matched_url($c, '(?<=New request URL: ).*');

    } elsif ($c =~ m/^Couldn\'t parse:/) {

        # XXX: These should probable be logged with LOG_LEVEL_ERROR
        # Couldn't parse: If-Modified-Since: Wed, 21 Mar 2007 16:34:50 GMT (crunching!)
        # Couldn't parse: at, 24 Mar 2007 13:46:21 GMT in If-Modified-Since: Sat, 24 Mar 2007 13:46:21 GMT (crunching!)
        $c =~ s@^(Couldn\'t parse)@$h{'error'}$1$h{'Standard'}@;

    } elsif ($c =~ /^Tagger \'([^\']*)\' added tag \'([^\']*)\'/ or
             $c =~ m/^Adding tag \'([^\']*)\' created by header tagger \'([^\']*)\'/) {

        # Adding tag 'GET request' created by header tagger 'method-man' (XXX: no longer used)
        # Tagger 'revalidation' added tag 'REVALIDATION-REQUEST'. No action bit update necessary.
        # Tagger 'revalidation' added tag 'REVALIDATION-REQUEST'. Action bits updated accordingly.

        # XXX: Save tag and tagger

        $c =~ s@(?<=^Tagger \')([^\']*)@$h{'tagger'}$1$h{'Standard'}@;
        $c =~ s@(?<=added tag \')([^\']*)@$h{'tag'}$1$h{'Standard'}@;
        $c =~ s@(?<=Action bits )(updated)@$h{'action-bits-update'}$1$h{'Standard'}@;
        $no_special_header_highlighting = 1;

    } elsif ($c =~ /^Tagger \'([^\']*)\' didn['']t add tag \'([^\']*)\'/) {

        # Tagger 'revalidation' didn't add tag 'REVALIDATION-REQUEST'. Tag already present
        # XXX: Save tag and tagger

        $c =~ s@(?<=^Tagger \')([^\']*)@$h{'tag'}$1$h{'Standard'}@;
        $c =~ s@(?<=didn['']t add tag \')([^\']*)@$h{'tagger'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^(?:scan:|Randomiz|addh:|Adding:|Removing:|Referer:|Modified:|Accept-Language header|[Cc]ookie)/
          or $c =~ m/^(Text mode is already enabled|Denied request with NULL byte|Replaced:|add-unique:)/
          or $c =~ m/^(Crunched (incoming|outgoing) cookie|Suppressed offer|Accepted the client)/
          or $c =~ m/^(addh-unique|Referer forged to)/
          or $c =~ m/^Downgraded answer to HTTP\/1.0/
          or $c =~ m/^Parameter: \+hide-referrer\{[^\}]*\} is a bad idea, but I don\'t care./
          or $c =~ m/^Referer (?:overwritten|replaced) with: Referer: / #XXX: should this be highlighted?
          or $c =~ m/^Referer crunched!/
          or $c =~ m/^crunched x-forwarded-for!/
          or $c =~ m/^crunched From!/
          or $c =~ m/^ modified$/
          or $c =~ m/^Content filtering is enabled. Crunching:/
          or $c =~ m/^force-text-mode overruled the client/
          or $c =~ m/^Server time in the future\./
          or $c =~ m/^content-disposition header crunched and replaced with:/i
          or $c =~ m/^Reducing white space in /
          or $c =~ m/^Ignoring single quote in /
          or $c =~ m/^Converting tab to space in /
          or $c =~ m/A HTTP\/1\.1 response without/
          or $c =~ m/Disabled filter mode on behalf of the client/
          or $c =~ m/Keeping the (?:server|client) header /
          or $c =~ m/Content modified with no Content-Length header set/
          or $c =~ m/^Appended client IP address to/
          or $c =~ m/^Removing 'Connection: close' to imply keep-alive./
          or $c =~ m/^keep-alive support is disabled/
          or $c =~ m/^Continue hack in da house/
          or $c =~ m/^Merged multiple header lines to:/
          or $c =~ m/^Added header: /
            )
    {
        # XXX: Some of these may need highlighting

        # Modified: User-Agent: Mozilla/5.0 (X11; U; SunOS i86pc; pl-PL; rv:1.8.1.1) Gecko/20070214 Firefox/2.0.0.1
        # Accept-Language header crunched and replaced with: Accept-Language: pl-pl
        # cookie 'Set-Cookie: eZSessionCookie=07bfec287c197440d299f81580593c3d; \
        #  expires=Thursday, 12-Apr-07 15:16:18 GMT; path=/' send by \
        #  http://wirres.net/article/articleview/4265/1/6/ appears to be using time format 1 (XXX: gone with the wind)
        # Cookie rewritten to a temporary one: Set-Cookie: NSC_gffe-iuuq-mc-wtfswfs=8efb33a53660;path=/
        # Text mode is already enabled
        # Denied request with NULL byte(s) turned into line break(s)
        # Replaced: 'Connection: Yo, home to Bel Air' with 'Connection: close'
        # addh-unique: Host: people.freebsd.org
        # Suppressed offer to compress content
        # Crunched incoming cookie -- yum!
        # Accepted the client's request to fetch without filtering.
        # Crunched outgoing cookie: Cookie: PREF=ID=6cf0abd347b30262:TM=1173357617:LM=1173357617:S=jZypyyJ7LPiwFi1_
        # addh-unique: Host: subkeys.pgp.net:11371
        # Referer forged to: Referer: http://10.0.0.1/
        # Downgraded answer to HTTP/1.0
        # Parameter: +hide-referrer{pille-palle} is a bad idea, but I don't care.
        # Referer overwritten with: Referer: pille-palle
        # Referer replaced with: Referer: pille-palle
        # crunched x-forwarded-for!
        # crunched From!
        #  modified # XXX: pretty stupid log message
        # Content filtering is enabled. Crunching: 'Range: 1234-5678' to prevent range-mismatch problems
        # force-text-mode overruled the client's request to fetch without filtering!
        # Server time in the future.
        # content-disposition header crunched and replaced with: content-disposition: filename=baz
        # Content-Disposition header crunched and replaced with: content-disposition: filename=baz
        # Reducing white space in 'X-LWS-Test: "This  is  quoted" this is not "this  is  " but " this again   is  not'
        # Ignoring single quote in 'X-LWS-Test: "This  is  quoted" this is not "this  is  " but "  this again   is  not'
        # Converting tab to space in 'X-LWS-Test:   "This  is  quoted" this   is  not "this  is  "  but  "\
        #  this again   is  not'
        # A HTTP/1.1 response without Connection header implies keep-alive.
        # Disabled filter mode on behalf of the client.
        # Keeping the server header 'Connection: keep-alive' around.
        # Keeping the client header 'Connection: close' around. The connection will not be kept alive.
        # Keeping the client header 'Connection: keep-alive' around. The connection will be kept alive if possible.
        # Content modified with no Content-Length header set. Creating a fake one for adjustment later on.
        # Appended client IP address to X-Forwarded-For: 10.0.0.2, 10.0.0.1
        # Removing 'Connection: close' to imply keep-alive.
        # keep-alive support is disabled. Crunching: Keep-Alive: 300.
        # Continue hack in da house.
        # Merged multiple header lines to: 'X-FORWARDED-PROTO: http X-HOST: 127.0.0.1'
        # Added header: Content-Encoding: deflate

    } elsif ($c =~ m/^scanning headers for:/) {

        return '' unless SHOW_SCAN_INTRO;

    } elsif ($c =~ m/^[Cc]runch(ing|ed)|crumble crunched:/) {
        # crunched User-Agent!
        # Crunching: Content-Encoding: gzip

        $c =~ s@(Crunching|crunched)@$h{$1}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Offending request data with NULL bytes turned into \'°\' characters:/) {

        # Offending request data with NULL bytes turned into '°' characters: °°n°°(°°°

        $c = h('warning') . $c . h('Standard');

    } elsif ($c =~ m/^(Transforming \")(.*?)(\" to \")(.*?)(\")/) {

        # Transforming "Proxy-Authenticate: Basic realm="Correos Proxy Server"" to\
        #  "Proxy-Authenticate: Basic realm="Correos Proxy Server""

       $c =~ s@(?<=^Transforming \")(.*)(?=\" to)@$h{'Header'}$1$h{'Standard'}@;
       $c =~ s@(?<=to \")(.*)(?=\")@$h{'Header'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Removing empty header/) {

        # Removing empty header
        # Ignore for now

    } elsif ($c =~ m/^Content-Type: .* not replaced/) {

        # Content-Type: application/octet-stream not replaced. It doesn't look like text.\
        #  Enable force-text-mode if you know what you're doing.
        # XXX: Could highlight more here.
        $c =~ s@(?<=^Content-Type: )(.*)(?= not replaced)@$h{'content-type'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^(Server|Client) keep-alive timeout is/) {

       # Server keep-alive timeout is 5. Sticking with 10.
       # Client keep-alive timeout is 20. Sticking with 10.

       $c =~ s@(?<=timeout is )(\d+)@$h{'Number'}$1$h{'Standard'}@;
       $c =~ s@(?<=Sticking with )(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Reducing keep-alive timeout/) {

       # Reducing keep-alive timeout from 60 to 10.

       $c =~ s@(?<= from )(\d+)@$h{'Number'}$1$h{'Standard'}@;
       $c =~ s@(?<= to )(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Killed all-caps Host header line: HOST:/) {

       # Killed all-caps Host header line: HOST: bestproxydb.com
       $c = highlight_matched_host($c, '(?<=HOST: )[^\s]+');
       $c = highlight_matched_pattern($c, 'HOST', 'HOST');

    } else {

        found_unknown_content($c);
    }

    # Highlight headers
    unless ($c =~ m/^Transforming/) {
        $c = highlight_known_headers($c) unless $no_special_header_highlighting;
    }

    return $c;
}

sub handle_loglevel_re_filter ($) {

    my $content = shift;
    my $c = $content;
    my $key;

    if ($c =~ m/^(?:re_)?filtering ([^\s]+) \(size (\d+)\) with (?:filter )?\'?([^\s]+?)\'? produced (\d+) hits \(new size (\d+)\)/) {

        # XXX: only the second version gets highlighted properly.
        # re_filtering www.lfk.de/favicon.ico (size 209) with filter untrackable-hulk produced 0 hits (new size 209).
        # filtering aci.blogg.de/ (size 37988) with 'blogg.de' produced 3 hits (new size 38057)
        $req{$t}{'content_source'} = $1;
        $req{$t}{'content_size'}   = $2;
        $req{$t}{'content_filter'} = $3;
        $req{$t}{'content_hits'}   = $4;
        $req{$t}{'new_content_size'} = $5;
        $req{$t}{'content_size_change'} = $req{$t}{'new_content_size'} - $req{$t}{'content_size'};
        #return '' if ($req{$t}{'content_hits'} == 0 && !cli_option_is_set('show-ineffective-filters'));
        if ($req{$t}{'content_hits'} == 0 and
            not (cli_option_is_set('show-ineffective-filters')
                 or ($req{$t}{'content_filter'} =~ m/^privoxy-filter-test$/))) {
                return '';
        }

        $c =~ s@(?<=\(size )(\d+)\)(?= with)@$h{'Number'}$1$h{'Standard'}@;
        $c =~ s@(?<=\(new size )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $c =~ s@(?<=produced )(\d+)(?= hits)@$h{'Number'}$1$h{'Standard'}@;

        $c =~ s@([^\s]+?)(\'? produced)@$h{'filter'}$1$h{'Standard'}$2@;
        $c = highlight_matched_host($c, '(?<=filtering )[^\s]+');

        $c =~ s@\.$@ @;
        $c .= "(" . $h{'Number'};
        $c .= "+" if ($req{$t}{'content_size_change'} >= 0);
        $c .= $req{$t}{'content_size_change'} . $h{'Standard'} . ")";
        $content = $c;

  } elsif ($c =~ /\.{3}$/
        and $c =~ m/^(?:re_)?filtering \'?(.*?)\'? \(size (\d*)\) with (?:filter )?\'?([^\s]*?)\'? ?\.{3}$/) {

        # Used by Privoxy 3.0.5 and 3.0.6:
        # XXX: Fill in ...
        # Used by Privoxy 3.0.7:
        # filtering 'Connection: close' (size 17) with 'generic-content-ads' ...

        $req{$t}{'filtered_header'} = $1;
        $req{$t}{'old_header_size'} = $2;
        $req{$t}{'header_filter_name'} = $3;

        unless (cli_option_is_set('show-ineffective-filters') or
                $req{$t}{'header_filter_name'} =~ m/^privoxy-filter-test$/) {
            return '';
        }
        $content =~ s@(?<=\(size )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $content =~ s@($req{$t}{'header_filter_name'})@$h{'filter'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^ ?\.\.\. ?produced (\d*) hits \(new size (\d*)\)\./) {

        # ...produced 0 hits (new size 23).
        #... produced 1 hits (new size 54).

        $req{$t}{'header_filter_hits'} = $1;
        $req{$t}{'new_header_size'} = $2;

        unless (cli_option_is_set('show-ineffective-filters') or
                (defined($req{$t}{'header_filter_name'}) and
                 $req{$t}{'header_filter_name'} =~ m/^privoxy-filter-test$/)) {

            if ($req{$t}{'header_filter_hits'} == 0 and
                not (defined($req{$t}{'header_filter_name'}) and
                 $req{$t}{'header_filter_name'} =~ m/^privoxy-filter-test$/)) {
                return '';
            }
            # Reformat including information from the intro
            $c = "'" . h('filter') . $req{$t}{'header_filter_name'} . h('Standard') . "'";
            $c .= " hit ";
            # XXX: Hide behind constant, it may be interesting if LOG_LEVEL_HEADER isn't enabled as well.
            # $c .= $req{$t}{'filtered_header'} . " ";
            $c .= h('Number') . $req{$t}{'header_filter_hits'}. h('Standard');
            $c .= ($req{$t}{'header_filter_hits'} == 1) ? " time, " : " times, ";

            if ($req{$t}{'old_header_size'} !=  $req{$t}{'new_header_size'}) {

                $c .= "changing size from ";
                $c .=  h('Number') . $req{$t}{'old_header_size'} . h('Standard');
                $c .= " to ";
                $c .= h('Number') . $req{$t}{'new_header_size'} . h('Standard');
                $c .= ".";

            } else {

                $c .= "keeping the size at " . $req{$t}{'old_header_size'};

            }

            # Highlight from last line (XXX: What?)
            # $c =~ s@(?<=produced )(\d+)@$h{'Number'}$1$h{'Standard'}@;
            # $c =~ s@($req{$t}{'header_filter_name'})@$h{'filter'}$1$h{'Standard'}@;

        } else {

           # XXX: Untested
           $c =~ s@(?<=produced )(\d+)@$h{'Number'}$1$h{'Standard'}@;
           $c =~ s@(?<=new size )(\d+)@$h{'Number'}$1$h{'Standard'}@;

        }
        $content = $c;

    } elsif ($c =~ m/^(Tagger|Filter) ([^\s]*) has empty joblist. Nothing to do./) {

        # Filter privoxy-filter-test has empty joblist. Nothing to do.
        # Tagger variable-test has empty joblist. Nothing to do.

        $content =~ s@(?<=$1 )([^\s]*)@$h{'filter'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^De-chunking successful. Shrunk from (\d+) to (\d+)/) {

        $req{$t}{'chunked-size'} = $1;
        $req{$t}{'dechunked-size'} = $2;
        $req{$t}{'dechunk-change'} = $req{$t}{'dechunked-size'} - $req{$t}{'chunked-size'};

        $content .= " (" . h('Number') . $req{$t}{'dechunk-change'} . h('Standard') . ")";

        $content =~ s@(?<=from )($req{$t}{'chunked-size'})@$h{'Number'}$1$h{'Standard'}@;
        $content =~ s@(?<=to )($req{$t}{'dechunked-size'})@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Decompression successful. Old size: (\d+), new size: (\d+)./) {

        # Decompression successful. Old size: 670, new size: 1166.

        $req{$t}{'size-compressed'} = $1;
        $req{$t}{'size-decompressed'} = $2;
        $req{$t}{'decompression-gain'} = $req{$t}{'size-decompressed'} - $req{$t}{'size-compressed'};

        $content =~ s@(?<=Old size: )($req{$t}{'size-compressed'})@$h{'Number'}$1$h{'Standard'}@;
        $content =~ s@(?<=new size: )($req{$t}{'size-decompressed'})@$h{'Number'}$1$h{'Standard'}@;

        # XXX: Create sub get_percentage()
        if ($req{$t}{'size-decompressed'}) {
            $req{$t}{'decompression-gain-percent'} =
                $req{$t}{'decompression-gain'} / $req{$t}{'size-decompressed'} * 100;

            $content .= " (saved: ";
            #$content .= h('Number') . $req{$t}{'decompression-gain'} . h('Standard');
            #$content .= "/";
            $content .= h('Number') . sprintf("%.2f%%", $req{$t}{'decompression-gain-percent'}) . h('Standard');
            $content .= ")";
        }

    } elsif ($c =~ m/^(Need to de-chunk first)/) {

        # Need to de-chunk first
        return '' if SUPPRESS_NEED_TO_DE_CHUNK_FIRST;

    } elsif ($c =~ m/^(Adding (?:dynamic )?re_filter job)/) {

        return ''  if (SUPPRESS_SUCCEEDED_FILTER_ADDITIONS && m/succeeded/);

        # Adding re_filter job ...
        # Adding dynamic re_filter job s@^(?:\w*)\s+.*\s+HTTP/\d\.\d\s*@IP-ADDRESS: $origin@D\
        #  to filter client-ip-address succeeded.

    } elsif ($c =~ m/^Compressed content from /) {

        # Compressed content from 29258 to 8630 bytes. Compression level: 3
        $content =~ s@(?<=from )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $content =~ s@(?<=to )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $content =~ s@(?<=level: )(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Reading in filter/) {

        return '' unless SHOW_FILTER_READIN_IN;

    } else {

        found_unknown_content($content);

    }

    return $content;
}

sub handle_loglevel_redirect ($) {

    my $c = shift;

    if ($c =~ m/^Decoding "([^""]*)"/) {

         $req{$t}{'original-destination'} = $1;
         $c = highlight_matched_path($c, '(?<=Decoding ")[^"]*');
         $c =~ s@\"@@g;

    } elsif ($c =~ m/^Checking/) {

         # Checking /_ylt=A0geu.Z76BRGR9k/**http://search.yahoo.com/search?p=view+odb+presentation+on+freebsd\
         #  &ei=UTF-8&xargs=0&pstart=1&fr=moz2&b=11 for redirects.

         # TODO: Change colour if really url-decoded
         $req{$t}{'decoded-original-destination'} = $1;
         $c = highlight_matched_path($c, '(?<=Checking ")[^"]*');
         $c =~ s@\"@@g;

    } elsif ($c =~ m/^pcrs command "([^""]*)" changed /) {

        # pcrs command "s@&from=rss@@" changed \
        #  "http://it.slashdot.org/article.pl?sid=07/03/02/1657247&from=rss"\
        #  to "http://it.slashdot.org/article.pl?sid=07/03/02/1657247" (1 hit).
        $c =~ s@(?<=pcrs command )"([^""]*)"@$h{'filter'}$1$h{'Standard'}@;
        $c = highlight_matched_url($c, '(?<=changed ")[^""]*');
        $c =~ s@(?<=changed )"([^""]*)"@$1@; # Remove quotes
        $c = highlight_matched_url($c, '(?<=to ")[^""]*');
        $c =~ s@(?<=to )"([^""]*)"@$1@; # Remove quotes
        $c =~ s@(\d+)(?= hits?)@$h{'hits'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^pcrs command "([^""]*)" didn\'t change/) {

        # pcrs command "s@^http://([^.]+?)/?$@http://www.bing.com/search?q=$1@" didn't \
        #  change "http://www.example.org/".
        $c =~ s@(?<=pcrs command )"([^""]*)"@$h{'filter'}$1$h{'Standard'}@;
        $c = highlight_matched_url($c, '(?<=change ")[^""]*');

    } elsif ($c =~ m/(^New URL is: )(.*)/) {

        # New URL is: http://it.slashdot.org/article.pl?sid=07/03/04/1511210
        # XXX: Use URL highlighter
        # XXX: Save?
        $c = $1 . h('rewritten-URL') . $2 . h('Standard');

    } elsif ($c =~ m/No pcrs command recognized, assuming that/) {
        # No pcrs command recognized, assuming that "http://config.privoxy.org/user-manual/favicon.png"\
        #  is already properly formatted.
        # XXX: assume the same?
        $c = highlight_matched_url($c, '(?<=assuming that \")[^"]*');

    } elsif ($c =~ m/^Percent-encoding redirect/) {

        # Percent-encoding redirect URL: http://www.example.org/\x02
        $c = highlight_matched_url($c, '(?<=redirect URL: ).*');

    } else {

        found_unknown_content($c);

    }

    return $c;
}

sub handle_loglevel_gif_deanimate ($) {

    my $content = shift;

    if ($content =~ m/Success! GIF shrunk from (\d+) bytes to (\d+)\./) {

        my $bytes_from = $1;
        my $bytes_to = $2;
        # Gif-Deanimate: Success! GIF shrunk from 205 bytes to 133.
        $content =~ s@$bytes_from@$h{'Number'}$bytes_from$h{'Standard'}@;
        # XXX: Do we need g in case of ($1 == $2)?
        $content =~ s@$bytes_to@$h{'Number'}$bytes_to$h{'Standard'}@;

    } elsif ($content =~ m/GIF (not) changed/) {

        # Gif-Deanimate: GIF not changed.
        return '' if SUPPRESS_GIF_NOT_CHANGED;
        $content =~ s@($1)@$h{'not'}$1$h{'Standard'}@;

    } elsif ($content =~ m/^failed! \(gif parsing\)/) {

        # failed! (gif parsing)
        # XXX: Replace this error message with something less stupid
        $content =~ s@(failed!)@$h{'error'}$1$h{'Standard'}@;

    } elsif ($content =~ m/^Need to de-chunk first/) {

        # Need to de-chunk first
        return '' if SUPPRESS_NEED_TO_DE_CHUNK_FIRST;

    } elsif ($content =~ m/^(?:No GIF header found|failed while parsing)/) {

        # No GIF header found (XXX: Did I ever commit this?)
        # failed while parsing 195 134747048 (XXX: never committed)

        # Ignore these for now

    } else {

        found_unknown_content($content);

    }

    return $content;
}

sub handle_loglevel_request ($) {

    my $content = shift;

    if ($content =~ m/crunch! /) {

        # config.privoxy.org/send-stylesheet crunch! (CGI Call)

        # Highlight crunch reasons
        foreach my $reason (keys %reason_colours) {
            $content =~ s@\(($reason)\)@$reason_colours{$reason}($1)$h{'Standard'}@g;
        }
        # Highlight request URL domain and ditch 'crunch!'
        $content = highlight_matched_pattern($content, 'request_', '[^ ]*(?= crunch!)');
        $content =~ s@ crunch!@@;

    } elsif ($content =~ m/\[too long, truncated\]$/) {

        # config.privoxy.org/edit-actions-submit?f=3&v=1176116716&s=7&Submit=Submit[...]&filter... [too long, truncated]
        $content = highlight_matched_pattern($content, 'request_', '^.*(?=\.\.\. \[too long, truncated\]$)');

    } elsif ($content =~ m/(.*)/) { # XXX: Pretty stupid

        # trac.vidalia-project.net/wiki/Volunteer?format=txt
        $content = h('request_') . $content . h('Standard');

    } else {  # XXX: Nop

        found_unknown_content($content);

    }

    return $content;
}

sub handle_loglevel_crunch ($) {

    my $content = shift;

    # Highlight crunch reason
    foreach my $reason (keys %reason_colours) {
        $content =~ s@($reason)@$reason_colours{$reason}$1$h{'Standard'}@g;
    }

    if ($content =~ m/\[too long, truncated\]$/) {

        # Blocked: config.privoxy.org/edit-actions-submit?f=3&v=1176116716&s=7&Submit=Submit\
        #  [...]&filter... [too long, truncated]
        $content = highlight_matched_pattern($content, 'request_', '^.*(?=\.\.\. \[too long, truncated\]$)');

    } else {

        # Blocked: http://ads.example.org/
        $content = highlight_matched_pattern($content, 'request_', '(?<=: ).*');
    }

    return $content;
}

sub handle_loglevel_connect ($) {

    my $c = shift;

    if ($c =~ m/^via [^\s]+ to: [^\s]+/) {

        # Connect: via 10.0.0.1:8123 to: www.example.org.noconnect

        $c = highlight_matched_host($c, '(?<=via )[^\s]+');
        $c = highlight_matched_host($c, '(?<=to: )[^\s]+');

    } elsif ($c =~ m/^connect to: .* failed: .*/) {

        # connect to: www.example.org.noconnect failed: Operation not permitted

        $c = highlight_matched_host($c, '(?<=connect to: )[^\s]+');

        $c =~ s@(?<=failed: )(.*)@$h{'error'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^to ([^\s]*)( successful)?$/) {

        # Connect: to www.nzherald.co.nz successful
        # Connect: to archiv.radiotux.de

        return '' if SUPPRESS_SUCCESSFUL_CONNECTIONS;
        $c = highlight_matched_host($c, '(?<=to )[^\s]+');

    } elsif ($c =~ m/^to ([^\s]*)$/) {

        # Connect: to lists.sourceforge.net:443

        $c = highlight_matched_host($c, '(?<=to )[^\s]+');

    } elsif ($c =~ m/^accepted connection from .*/ or
             $c =~ m/^OK/) {

        # accepted connection from 10.0.0.1
        # Privoxy 3.0.6 and earlier just say:
        # OK
        return '' if SUPPRESS_ACCEPTED_CONNECTIONS;
        $c = highlight_matched_host($c, '(?<=connection from ).*');

    } elsif ($c =~ m/^write header to: .* failed:/) {

        # write header to: 10.0.0.1 failed: Broken pipe

        $c = highlight_matched_host($c, '(?<=write header to: )[^\s]*');
        $c =~ s@(?<=failed: )(.*)@$h{'Error'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^write header to client failed:/) {

        # write header to client failed: Broken pipe
        # XXX: Stil in use?
        $c =~ s@(?<=failed: )(.*)@$h{'Error'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^socks4_connect:/) {

        # socks4_connect: SOCKS request rejected or failed.
        $c =~ s@(?<=socks4_connect: )(.*)@$h{'Error'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Listening for new connections/ or
             $c =~ m/^accept connection/) {
        # XXX: Highlight?
        # Privoxy versions above 3.0.6 say:
        # Listening for new connections ...
        # earlier versions say:
        # accept connection ...
        return '';

    } elsif ($c =~ m/^accept failed:/) {

        $c =~ s@(?<=accept failed: )(.*)@$h{'Error'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Overriding forwarding settings/) {

        # Overriding forwarding settings based on 'forward 10.0.0.1:8123'
        $c =~ s@(?<=based on \')(.*)(?=\')@$h{'configuration-line'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Denying suspicious CONNECT request from/) {

        # Denying suspicious CONNECT request from 10.0.0.1
        $c = highlight_matched_host($c, '(?<=from )[^\s]+'); # XXX: not an URL

    } elsif ($c =~ m/^socks5_connect:/) {

        $c =~ s@(?<=socks5_connect: )(.*)@$h{'error'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Created new connection to/) {

        # Created new connection to www.privoxy.org:80 on socket 11.
        $c = highlight_matched_host($c, '(?<=connection to )[^\s]+');
        $c =~ s@(?<=on socket )(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Found reusable socket/) {

        # Found reusable socket 9 for www.privoxy.org:80 in slot 0.
        # 3.0.15 and later:
        # Found reusable socket 8 for www.privoxy.org:80 in slot 2.\
        #  Timestamp made 0 seconds ago. Timeout: 1. Latency: 0.
        $c =~ s@(?<=Found reusable socket )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $c = highlight_matched_host($c, '(?<=for )[^\s]+');
        $c =~ s@(?<=in slot )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $c =~ s@(?<=made )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $c =~ s@(?<=Timeout: )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $c =~ s@(?<=Latency: )(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Marking open socket/) {

        # Marking open socket 9 for www.privoxy.org:80 in slot 0 as unused.
        $c =~ s@(?<=Marking open socket )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $c = highlight_matched_host($c, '(?<=for )[^\s]+');
        $c =~ s@(?<=in slot )(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^No reusable/) {

        # No reusable socket for addons.mozilla.org:443 found. Opening a new one.
        $c = highlight_matched_host($c, '(?<=for )[^\s]+');

    } elsif ($c =~ m/^(Remembering|Forgetting) socket/) {

        # Remembering socket 13 for www.privoxy.org:80 in slot 0.
        # Forgetting socket 38 for www.privoxy.org:80 in slot 5.

        $c =~ s@(?<=socket )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $c = highlight_matched_host($c, '(?<=for )[^\s]+');
        $c =~ s@(?<=in slot )(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Socket/) {

        # Socket 16 already forgotten or never remembered.
        $c =~ s@(?<=Socket )(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^The connection to/) {

        # The connection to www.privoxy.org:80 in slot 6 timed out. Closing socket 19. Timeout is: 61.
        # 3.0.15 and later:
        # The connection to 1.bp.blogspot.com:80 in slot 0 timed out. Closing socket 5.\
        #  Timeout is: 1. Assumed latency: 4.
        # The connection to 10.0.0.1:80 in slot 0 is no longer usable. Closing socket 4.
        $c = highlight_matched_host($c, '(?<=connection to )[^\s]+');
        $c =~ s@(?<=in slot )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $c =~ s@(?<=Closing socket )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $c =~ s@(?<=Timeout is: )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $c =~ s@(?<=Assumed latency: )(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Stopped waiting for the request line./) {

        # Stopped waiting for the request line. Timeout: 121.
        $c =~ s@(?<=Timeout: )(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Waiting for \d/) {

        # Waiting for 1 connections to timeout.
        $c =~ s@(?<=^Waiting for )(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Initialized/) {

        # Initialized 20 socket slots.
        $c =~ s@(?<=Initialized )(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Done reading from server/) {

        # Done reading from server. Expected content length: 24892. \
        #  Actual content length: 24892. Most recently received: 4412.
        # 3.0.15 and later:
        # Done reading from server. Expected content length: 24892. \
        #  Actual content length: 24892. Bytes most recently read: 4412.
        # Done reading from server. Content length: 6018 as expected. \
        #  Bytes most recently read: 294.
        $c =~ s@(?<=ontent length: )(\d+)@$h{'Number'}$1$h{'Standard'}@g;
        $c =~ s@(?<=received: )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $c =~ s@(?<=read: )(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Continuing buffering headers/) {

        # Continuing buffering headers. byte_count: 19. header_offset: 517. len: 536.
        $c =~ s@(?<=byte_count: )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $c =~ s@(?<=header_offset: )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $c =~ s@(?<=len: )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        # 3.0.15 and later:
        # Continuing buffering headers. Bytes most recently read: %d.
        $c =~ s@(?<=read: )(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Received \d+ bytes while/) {

        # Received 206 bytes while expecting 12103.
        $c =~ s@(?<=Received )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $c =~ s@(?<=expecting )(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^(Rejecting c|C)onnection from/) {

        # Connection from 81.163.28.218 dropped due to ACL
        # Rejecting connection from 178.63.152.227. Maximum number of connections reached.
        $c =~ s@(?<=onnection from )((?:\d+\.?){3}\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^(?:Reusing|Closing) server socket \d./ or
             $c =~ m/^No additional client request/) {

        # Reusing server socket 4. Opened for 10.0.0.1.
        # Closing server socket 2. Opened for 10.0.0.1.
        # No additional client request received in time. \
        #  Closing server socket 4, initially opened for 10.0.0.1.
        # No additional client request received in time on socket 29.

        $c =~ s@(?<= socket )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $c = highlight_matched_host($c, '(?<=for )[^\s]+(?=\.$)');

    } elsif ($c =~ m/^Connected to /) {

        # Connected to tor-jail[10.0.0.2]:9050.

        $c = highlight_matched_host($c, '(?<=\[)[^\]]+');
        $c = highlight_matched_host($c, '(?<=Connected to )[^\[\s]+');
        $c =~ s@(?<=\]:)(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Could not connect to /) {

        # Could not connect to [10.0.0.1]:80.

        $c = highlight_matched_host($c, '(?<=\[)[^\]]+');
        $c =~ s@(?<=\]:)(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Waiting for the next client request/ or
             $c =~ m/^The connection on server socket/ or
             $c =~ m/^Client request arrived in time /) {

        # Waiting for the next client request on socket 3. Keeping the server \
        #  socket 12 to a.fsdn.com open.
        # The connection on server socket 6 to upload.wikimedia.org isn't reusable. Closing.
        # Used by Privoxy 3.0.18 and later:
        # Client request arrived in time on socket 21.
        # Used by earlier version:
        # Client request arrived in time or the client closed the connection on socket 12.

        $c =~ s@(?<=on socket )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $c =~ s@(?<=server socket )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $c = highlight_matched_host($c, '(?<=to )[^\s]+');

    } elsif ($c =~ m/^Marking the server socket/) {

        # Marking the server socket 7 tainted.

        $c =~ s@(?<=server socket )(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Reduced expected bytes to /) {

        # Reduced expected bytes to 0 to account for the 1542 ones we already got.
        $c =~ s@(?<=bytes to )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $c =~ s@(?<=for the )(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^The client closed socket /) {

        # The client closed socket 2 while the server socket 4 is still open.
        $c =~ s@(?<=closed socket )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $c =~ s@(?<=server socket )(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Expected client content length set /) {

        # Expected client content length set to 667325411 after reading 4999 bytes.
        $c =~ s@(?<=set to )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $c =~ s@(?<=reading )(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Reducing expected bytes to /) {

        # Reducing expected bytes to 0. Marking the server socket tainted after throwing 4 bytes away.
        $c =~ s@(?<=bytes to )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $c =~ s@(?<=after throwing )(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Waiting for up to /) {

        # Waiting for up to 4999 bytes from the client.
        $c =~ s@(?<=up to )(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Looks like we / or
             $c =~ m/^Unsetting keep-alive flag/ or
             $c =~ m/^No connections to wait/ or
             $c =~ m/^Complete client request received/ or
             $c =~ m/^Possible pipeline attempt detected./ or
             $c =~ m/^POST request detected. The connection will not be kept alive./ or
             $c =~ m/^The server still wants to talk, but the client hung up on us./ or
             $c =~ m/^The server didn't specify how long the connection will stay open/ or
             $c =~ m/^There might be a request body. The connection will not be kept alive/ or
             $c =~ m/^Stopping to watch the client socket. There's already another request waiting./ or
             $c =~ m/^Done reading from the client\.$/) {

        # Looks like we reached the end of the last chunk. We better stop reading.
        # Looks like we read the end of the last chunk together with the server \
        #  headers. We better stop reading.
        # Looks like we got the last chunk together with the server headers. \
        #  We better stop reading.
        # Unsetting keep-alive flag.
        # No connections to wait for left.
        # Client request arrived in time or the client closed the connection.
        # Complete client request received
        # Possible pipeline attempt detected. The connection will not be \
        #  kept alive and we will only serve the first request.
        # POST request detected. The connection will not be kept alive.
        # The server still wants to talk, but the client hung up on us.
        # The server didn't specify how long the connection will stay open. Assume it's only a second.
        # There might be a request body. The connection will not be kept alive.
        # Stopping to watch the client socket. There's already another request waiting.
        # Done reading from the client\.

    } else {

        found_unknown_content($c);

    }

    return $c;
}


sub handle_loglevel_info ($) {

    my $c = shift;

    if ($c =~ m/^Rewrite detected:/) {

        # Rewrite detected: GET http://10.0.0.2:88/blah.txt HTTP/1.1
        $c = highlight_matched_request_line($c, '(?<=^Rewrite detected: ).*');

    } elsif ($c =~ m/^Decompress(ing deflated|ion didn)/ or
             $c =~ m/^Compressed content detected/ or
             $c =~ m/^SDCH-compressed content detected/ or
             $c =~ m/^Tagger/
            ) {
        # Decompressing deflated iob: 117
        # Decompression didn't result in any content.
        # Compressed content detected, content filtering disabled. Consider recompiling Privoxy\
        #  with zlib support or enable the prevent-compression action.
        # SDCH-compressed content detected, content filtering disabled.\
        #  Consider suppressing SDCH offers made by the client.
        # Tagger 'complete-url' created empty tag. Ignored.

        # Ignored for now

    } elsif ($c =~ m/^(Re)?loading configuration file /) {

        # loading configuration file '/usr/local/etc/privoxy/config':
        # Reloading configuration file '/usr/local/etc/privoxy/config'
        $c =~ s@(?<=loading configuration file \')([^\']*)@$h{'file'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Loading (actions|filter) file: /) {

        # Loading actions file: /usr/local/etc/privoxy/default.action
        # Loading filter file: /usr/local/etc/privoxy/default.filter
        $c =~ s@(?<= file: )(.*)$@$h{'file'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^exiting by signal/) {

        # exiting by signal 15 .. bye
        $c =~ s@(?<=exiting by signal )(\d+)@$h{'signal'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Privoxy version/) {

        # Privoxy version 3.0.7
        $c =~ s@(?<=^Privoxy version )(\d+\.\d+\.\d+)$@$h{'version'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Program name: /) {

        # Program name: /usr/local/sbin/privoxy
        $c =~ s@(?<=Program name: )(.*)@$h{'program-name'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Listening on port /) {

        # Listening on port 8118 on IP address 10.0.0.1
        $c =~ s@(?<=Listening on port )(\d+)@$h{'port'}$1$h{'Standard'}@;
        $c =~ s@(?<=on IP address )(.*)@$h{'ip-address'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^\(Re-\)Open(?:ing)? logfile/) {

        # (Re-)Open logfile /var/log/privoxy/privoxy.log
        $c =~ s@(?<=Open logfile )(.*)@$h{'file'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^(Request from|Malformed server response detected)/) {

        # Request from 10.0.0.1 denied. limit-connect{,} doesn't allow CONNECT requests to port 443.
        # Request from 10.0.0.1 marked for blocking. limit-connect{,} doesn't allow CONNECT requests to port 443.
        # 3.0.18 and later:
        # Request from 10.0.0.1 marked for blocking. limit-connect{0} doesn't allow CONNECT requests to www.example.org:443
        # Malformed server response detected. Downgrading to HTTP/1.0 impossible.

        $c =~ s@(?<=Request from )([^\s]*)@$h{'ip-address'}$1$h{'Standard'}@;
        $c =~ s@(denied|blocking)@$h{'warning'}$1$h{'Standard'}@;
        $c =~ s@(CONNECT)@$h{'method'}$1$h{'Standard'}@;
        $c =~ s@(?<=to port )(\d+)@$h{'port'}$1$h{'Standard'}@;
        $c =~ s@(?<=to )([^\s]+)@$h{'request_'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Status code/) {

        # Status code 304 implies no body.
        $c =~ s@(?<=Status code )(\d+)@$h{'status-code'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Method/) {

        # Method HEAD implies no body.
        $c =~ s@(?<=Method )([^\s]+)@$h{'method'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Buffer limit reached while extending /) {

        # Buffer limit reached while extending the buffer (iob). Needed: 4197470. Limit: 4194304
        $c =~ s@(?<=Needed: )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $c =~ s@(?<=Limit: )(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^File modification detected: /) {

        # File modification detected: /usr/local/etc/privoxy/user-agent.action
        $c =~ s@(?<= detected: )(.*)$@$h{'file'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^No logfile configured/ or
             $c =~ m/^Malformerd HTTP headers detected and MS IIS5 hack enabled/ or
             $c =~ m/^Invalid \"chunked\" transfer/ or
             $c =~ m/^Support for/ or
             $c =~ m/^Flushing header and buffers/ or
             $c =~ m/^Can not resolve/
             ) {

        # No logfile configured. Please enable it before reporting any problems.
        # Malformerd HTTP headers detected and MS IIS5 hack enabled. Expect an invalid \
        #  response or even no response at all.
        # No logfile configured. Logging disabled.
        # Invalid "chunked" transfer encoding detected and ignored.
        # Support for 'Connection: keep-alive' is experimental, incomplete and\
        #  known not to work properly in some situations.
        # Flushing header and buffers. Stepping back from filtering.
        # Can not resolve doesnotexist: hostname nor servname provided, or not known

    } else {

        found_unknown_content($c);

    }

    return $c;
}

sub handle_loglevel_cgi ($) {

    my $c = shift;

    if ($c =~ m/^Granting access to/) {

        #Granting access to http://config.privoxy.org/send-stylesheet, referrer http://p.p/ is trustworthy.

    } elsif ($c =~ m/^Substituting: s(.)/) {

        # Substituting: s/@else-not-FEATURE_ZLIB@.*@endif-FEATURE_ZLIB@//sigTU
        # XXX: prone to span several lines

        my $delimiter = $1;
        #$c =~ s@(?<=failed: )(.*)@$h{'error'}$1$h{'Standard'}@;
        $c =~ s@(?!<=\\)($delimiter)@$h{'pcrs-delimiter'}$1$h{'Standard'}@g; # XXX: Too aggressive
        #$c =~ s@(?!<=\\)($1)@$h{'pcrs-delimiter'}$1$h{'Standard'}@g;
    }

    return $c;
}

sub handle_loglevel_force ($) {

    my $c = shift;

    if ($c =~ m/^Ignored force prefix in request:/) {

        # Ignored force prefix in request: "GET http://10.0.0.1/PRIVOXY-FORCE/block HTTP/1.1"
        $c =~ s@^(Ignored)@$h{'ignored'}$1$h{'Standard'}@;
        $c = highlight_matched_request_line($c, '(?<=request: ")[^"]*');

    } elsif ($c =~ m/^Enforcing request:/) {

        # Enforcing request: "GET http://10.0.0.1/block HTTP/1.1".
        $c = highlight_matched_request_line($c, '(?<=request: ")[^"]*');

    } else {

        found_unknown_content($c);

    }

    return $c;
}

sub handle_loglevel_error ($) {

    my $c = shift;

    if ($c =~ m/^(?:Empty|No) server or forwarder response received on socket \d+\./) {

        # Empty server or forwarder response received on socket 4.
        # Empty server or forwarder response received on socket 3. \
        #  Closing client socket 15 without sending data.
        # Used by Privoxy 3.0.18 and later:
        # No server or forwarder response received on socket 8. \
        #  Closing client socket 10 without sending data.

        $c =~ s@(?<=on socket )(\d+)@$h{'Number'}$1$h{'Standard'}@;
        $c =~ s@(?<=client socket )(\d+)@$h{'Number'}$1$h{'Standard'}@;

    } elsif ($c =~ m/^Didn't receive data in time:/) {

        # Didn't receive data in time: a.fsdn.com:443
        $c =~ s@(?<=in time: )(.*)@$h{'destination'}$1$h{'Standard'}@;
    }

    # XXX: There are probably more messages that deserve highlighting.

    return $c;
}


sub handle_loglevel_ignore ($) {
    return shift;
}

sub gather_loglevel_request_stats ($$) {
    my $c = shift;
    my $thread = shift;
    our %stats;

    $stats{requests}++;
}

sub gather_loglevel_crunch_stats ($$) {
    my $c = shift;
    my $thread = shift;
    our %stats;

    $stats{requests}++;
    $stats{crunches}++;

    if ($c =~ m/^Redirected:/) {
        # Redirected: http://www.example.org/http://p.p/
        $stats{'fast-redirections'}++;

    } elsif ($c =~ m/^Blocked:/) {
        # Blocked: blogger.googleusercontent.com:443
        $stats{'blocked'}++;
    }
}


sub gather_loglevel_error_stats ($$) {

    my $c = shift;
    my $thread = shift;
    our %stats;
    our %thread_data;

    if ($c =~ m/^Empty server or forwarder response received on socket \d+./) {

        # Empty server or forwarder response received on socket 4.
        $stats{'empty-responses'}++;
        if ($thread_data{$thread}{'new_connection'}) {
            $stats{'empty-responses-on-new-connections'}++;
        } else {
            $stats{'empty-responses-on-reused-connections'}++;
        }
    }
}

sub gather_loglevel_connect_stats ($$) {

    my ($c, $thread) = @_;
    our %thread_data;
    our %stats;

    if ($c =~ m/^via ([^\s]+) to: [^\s]+/) {

        # Connect: via 10.0.0.1:8123 to: www.example.org.noconnect
        $thread_data{$thread}{'forwarder'} = $1; # XXX: is this missue?

    } elsif ($c =~ m/^to ([^\s]*)$/) {

        # Connect: to lists.sourceforge.net:443

        $thread_data{$thread}{'forwarder'} = 'direct connection';

    } elsif ($c =~ m/^Created new connection to/) {

        # Created new connection to www.privoxy.org:80 on socket 11.

        $thread_data{$thread}{'new_connection'} = 1;

    } elsif ($c =~ m/^Reusing server socket \d./ or
             $c =~ m/^Found reusable socket/) {

        # Reusing server socket 4. Opened for 10.0.0.1.
        # Found reusable socket 9 for www.privoxy.org:80 in slot 0.

        $thread_data{$thread}{'new_connection'} = 0;
        $stats{'reused-connections'}++;
    }
}

sub gather_loglevel_header_stats ($$) {

    my ($c, $thread) = @_;
    our %stats;

    if ($c =~ m/^A HTTP\/1\.1 response without/ or
        $c =~ m/^Keeping the server header 'Connection: keep-alive' around./)
    {
        # A HTTP/1.1 response without Connection header implies keep-alive.
        # Keeping the server header 'Connection: keep-alive' around.
        $stats{'server-keep-alive'}++;

    } elsif ($c =~ m/^scan: ((\w+) (.+) (HTTP\/\d\.\d))/) {

        # scan: HTTP/1.1 200 OK
        $stats{'method'}{$2}++;
        $stats{'ressource'}{$3}++;
        $stats{'http-version'}{$4}++;

    } elsif ($c =~ m/^scan: Host: ([^\s]+)/) {

        # scan: Host: p.p
        $stats{'hosts'}{$1}++;
    }
}

sub init_stats () {
    our %stats = (
        requests => 0,
        crunches => 0,
        'server-keep-alive' => 0,
        'reused-connections' => 0,
        'empty-responses' => 0,
        'empty-responses-on-new-connections' => 0,
        'empty-responses-on-reused-connections' => 0,
        'fast-redirections' => 0,
        'blocked' => 0,
        'reused-connections' => 0,
        'server-keep-alive' => 0,
        );
}

sub get_percentage ($$) {
    my $big = shift;
    my $small = shift;
    return "NaN" if ($big eq 0);
    return sprintf("%.2f%%", $small / $big * 100);
}

sub print_stats () {

    our %stats;
    our %cli_options;
    my $new_connections = $stats{requests} - $stats{crunches} - $stats{'reused-connections'};
    my $outgoing_requests = $stats{requests} - $stats{crunches};

    if ($stats{requests} eq 0) {
        print "No requests yet.\n";
        return;
    }

    print "Client requests total: " . $stats{requests} . "\n";
    print "Crunches: " . $stats{crunches} . " (" .
        get_percentage($stats{requests}, $stats{crunches}) . ")\n";
    print "Blocks: " . $stats{'blocked'} . " (" .
        get_percentage($stats{requests}, $stats{'blocked'}) . ")\n";
    print "Fast redirections: " . $stats{'fast-redirections'} . " (" .
        get_percentage($stats{requests}, $stats{'fast-redirections'}) . ")\n";
    print "Outgoing requests: " . $outgoing_requests . " (" .
        get_percentage($stats{requests}, $outgoing_requests) . ")\n";
    print "Server keep-alive offers: " . $stats{'server-keep-alive'} . " (" .
        get_percentage($stats{requests}, $stats{'server-keep-alive'}) . ")\n";
    print "New outgoing connections: " . $new_connections . " (" .
        get_percentage($stats{requests}, $new_connections) . ")\n";
    print "Reused connections: " . $stats{'reused-connections'} . " (" .
        get_percentage($stats{requests}, $stats{'reused-connections'}) .
        "; server offers accepted: " .
        get_percentage($stats{'server-keep-alive'}, $stats{'reused-connections'}) . ")\n";
    print "Empty responses: " . $stats{'empty-responses'} . " (" .
        get_percentage($stats{requests}, $stats{'empty-responses'}) . ")\n";
    print "Empty responses on new connections: "
         . $stats{'empty-responses-on-new-connections'} . " (" .
        get_percentage($stats{requests}, $stats{'empty-responses-on-new-connections'})
        . ")\n";
    print "Empty responses on reused connections: " .
        $stats{'empty-responses-on-reused-connections'} . " (" .
        get_percentage($stats{requests}, $stats{'empty-responses-on-reused-connections'}) .
        ")\n";

    if ($stats{method} eq 0) {
        print "No response lines parsed yet yet.\n";
        return;
    }
    print "Method distribution:\n";
    foreach my $method (sort {$stats{'method'}{$b} <=> $stats{'method'}{$a}} keys %{$stats{'method'}}) {
        printf "%8d : %-8s\n", $stats{'method'}{$method}, $method;
    }
    print "Client HTTP versions:\n";
    foreach my $http_version (sort {$stats{'http-version'}{$b} <=> $stats{'http-version'}{$a}} keys %{$stats{'http-version'}}) {
        printf "%d : %s\n",  $stats{'http-version'}{$http_version}, $http_version;
    }

    if ($cli_options{'url-statistics-threshold'} == 0) {
        print "URL statistics are disabled. Increase --url-statistics-threshold to enable them.\n";
    } else {
        print "Requested URLs:\n";
        foreach my $ressource (sort {$stats{'ressource'}{$b} <=> $stats{'ressource'}{$a}} keys %{$stats{'ressource'}}) {
            if ($stats{'ressource'}{$ressource} < $cli_options{'url-statistics-threshold'}) {
                print "Skipped statistics for URLs below the treshold.\n";
                last;
            }
            printf "%d : %s\n", $stats{'ressource'}{$ressource}, $ressource;
        }
    }

    if ($cli_options{'host-statistics-threshold'} == 0) {
        print "Host statistics are disabled. Increase --host-statistics-threshold to enable them.\n";
    } else {
        print "Requested Hosts:\n";
        foreach my $host (sort {$stats{'hosts'}{$b} <=> $stats{'hosts'}{$a}} keys %{$stats{'hosts'}}) {
            if ($stats{'hosts'}{$host} < $cli_options{'host-statistics-threshold'}) {
                print "Skipped statistics for Hosts below the treshold.\n";
                last;
            }
            printf "%d : %s\n", $stats{'hosts'}{$host}, $host;
        }
    }
}


################################################################################
# Functions that actually print stuff
################################################################################

sub print_clf_message () {

    our ($ip, $timestamp, $request_line, $status_code, $size);
    my $output = '';

    return if DEBUG_SUPPRESS_LOG_MESSAGES;

    # Rebuild highlighted
    $output .= $h{'Number'} . $ip . $h{'Standard'};
    $output .= " - - ";
    $output .= "[" . $h{'Timestamp'} . $timestamp . $h{'Standard'} . "]";
    $output .= " ";
    $output .= "\"" . highlight_request_line("$request_line") . "\"";
    $output .= " ";
    $output .= $h{'Status'} . $status_code . $h{'Standard'};
    $output .= " ";
    $output .= $h{'Number'} . $size . $h{'Standard'};
    $output .= $line_end;

    print $output;
}

sub print_non_clf_message ($) {

    my $content = shift;
    my $msec_string = "." . $req{$t}{'msecs'} unless $no_msecs_mode;
    my $line_start = $html_output_mode ? '' : $h{"Standard"};

    return if DEBUG_SUPPRESS_LOG_MESSAGES;

    print $line_start
        . $time_colours[$time_colour_index % 2]
        . $req{$t}{'time-stamp'}
        . $msec_string
        . $h{Standard} . " "
        . $thread_colours{$t}
        . $t
        . $h{Standard}
        . " "
        . $h{$req{$t}{'log-level'}}
        . $req{$t}{'log-level'}
        . $h{Standard}
        . ": "
        . $content
        . $line_end;
}

sub shorten_thread_id ($) {

    my $thread_id = shift;

    our %short_thread_ids;
    our $max_threadid;

    unless (defined $short_thread_ids{$thread_id}) {
        $short_thread_ids{$thread_id} = sprintf "%.3d", $max_threadid++;
    }

    return $short_thread_ids{$thread_id}
}

sub parse_loop () {

    my ($day, $time_stamp, $thread, $log_level, $content, $c, $msecs);
    my $last_msecs  = 0;
    my $last_thread = 0;
    my $last_timestamp = 0;
    my $filters_that_did_nothing;
    my $key;
    my $time_colour;
    $time_colour = paint_it('white');

    my %log_level_handlers = (
        'Re-Filter'         => \&handle_loglevel_re_filter,
        'Header'            => \&handle_loglevel_header,
        'Connect'           => \&handle_loglevel_connect,
        'Redirect'          => \&handle_loglevel_redirect,
        'Request'           => \&handle_loglevel_request,
        'Crunch'            => \&handle_loglevel_crunch,
        'Gif-Deanimate'     => \&handle_loglevel_gif_deanimate,
        'Info'              => \&handle_loglevel_info,
        'CGI'               => \&handle_loglevel_cgi,
        'Force'             => \&handle_loglevel_force,
        'Error'             => \&handle_loglevel_error,
        'Fatal error'       => \&handle_loglevel_ignore,
        'Writing'           => \&handle_loglevel_ignore,
        'Received'          => \&handle_loglevel_ignore,
        'Unknown log level' => \&handle_loglevel_ignore,
    );

    while (<>) {

        if (m/^(\d{4}-\d{2}-\d{2}|\w{3} \d{2}) (\d\d:\d\d:\d\d)\.?(\d+)? (?:Privoxy\()?([^\)\s]*)[\)]? ([\w -]*): (.*?)\r?$/) {
            $thread = $t = ($shorten_thread_ids) ? shorten_thread_id($4) : $4;
            $req{$t}{'day'} = $day = $1;
            $req{$t}{'time-stamp'} = $time_stamp = $2;
            $req{$t}{'msecs'} = $msecs = $3 ? $3 : 0; # Only the cool kids have micro second resolution;
            $req{$t}{'log-level'} = $log_level = $5;
            $req{$t}{'content'} = $content = $c = $6;
            $req{$t}{'log-message'} = $_;
            $no_special_header_highlighting = 0;

            if (defined($log_level_handlers{$log_level})) {

                $content = $log_level_handlers{$log_level}($content);

            } else {

                die "No handler found for log level \"$log_level\"\n";
            }

            # Highlight Truncations
            if (length($_) > 4000) {
                $content =~ s@(too long, truncated)]$@$h{'Truncation'}$1$h{'Standard'}]@g;
            }

            next unless $content;

            # Register threads to keep the colour constant
            if (!defined($thread_colours{$thread})) {
                $thread_colours{$thread} = $all_colours[$thread_colour_index % @all_colours];
                $thread_colour_index++;
            }

            # Switch timestamp colour if timestamps differ
            if (($msecs ne $last_msecs) || ($time_stamp ne $last_timestamp)) {
               debug_message("Tick tack!") if DEBUG_TICKS;
               $time_colour = $time_colours[$time_colour_index % 2];
               $time_colour_index++;
               $last_msecs = $msecs;
               $last_timestamp = $time_stamp;
            }

            $last_thread = $thread;

            print_non_clf_message($content);

        } elsif (m/^((?:\d+\.\d+\.\d+\.\d+|[:\d]+)) - - \[(.*)\] "(.*)" (\d+) (\d+)/) {

            # LOG_LEVEL_CLF lines look like this
            # 61.152.239.32 - - [04/Mar/2007:18:28:23 +0100] "GET \
            #  http://ad.yieldmanager.com/imp?z=1&Z=120x600&s=109339&u=http%3A%2F%2Fwww.365loan.co.uk%2F&r=1\
            #  HTTP/1.1" 403 1730
            our ($ip, $timestamp, $request_line, $status_code, $size) = ($1, $2, $3, $4, $5);

            print_clf_message();

        } else {

            # Some Privoxy log messages span more than one line,
            # usually to dump lots of content that doesn't need any syntax highlighting.
            # XXX: add mechanism to forward these lines to the right handler anyway.
            chomp();
            unless (DEBUG_SUPPRESS_LOG_MESSAGES or (SUPPRESS_EMPTY_LINES and m/^\s+$/)) {
                print and print get_line_end(); # unless (SUPPRESS_EMPTY_LINES and m/^\s+$/);
            }
        }
    }
}

sub stats_loop () {

    my ($day, $time_stamp, $msecs, $thread, $log_level, $content);
    my %log_level_handlers = (
         'Re-Filter'         => \&handle_loglevel_ignore,
         'Header'            => \&gather_loglevel_header_stats,
         'Connect'           => \&gather_loglevel_connect_stats,
         'Redirect'          => \&handle_loglevel_ignore,
         'Request'           => \&gather_loglevel_request_stats,
         'Crunch'            => \&gather_loglevel_crunch_stats,
         'Gif-Deanimate'     => \&handle_loglevel_ignore,
         'Info'              => \&handle_loglevel_ignore,
         'CGI'               => \&handle_loglevel_ignore,
         'Force'             => \&handle_loglevel_ignore,
         'Error'             => \&gather_loglevel_error_stats,
         'Fatal error'       => \&handle_loglevel_ignore,
         'Writing'           => \&handle_loglevel_ignore,
         'Received'          => \&handle_loglevel_ignore,
         'Unknown log level' => \&handle_loglevel_ignore
    );

    while (<>) {
        if (m/^(\d{4}-\d{2}-\d{2}|\w{3} \d{2}) (\d\d:\d\d:\d\d)\.?(\d+)? (?:Privoxy\()?([^\)\s]*)[\)]? ([\w -]*): (.*?)\r?$/) {
            $day = $1;
            $time_stamp = $2;
            $msecs = $3 ? $3 : 0;
            $thread = $4;
            $log_level = $5;
            $content = $6;

            if (defined($log_level_handlers{$log_level})) {

                $content = $log_level_handlers{$log_level}($content, $thread);

            } else {

                die "No handler found for log level \"$log_level\"\n";

            }
        }
    }

    print_stats();

}

sub unbreak_lines_only_loop() {
    my $log_messages_reached = 0;
    while (<>) {
        chomp;

            # Log level other than LOG_LEVEL_CLF?
        if (m/^(\d{4}-\d{2}-\d{2}|\w{3} \d{2}) (\d\d:\d\d:\d\d)\.?(\d+)? (?:Privoxy\()?([^\)\s]*)[\)]? ([\w -]*): (.*?)\r?$/ or
            # LOG_LEVEL_CLF?
            m/^((?:\d+\.\d+\.\d+\.\d+)) - - \[(.*)\] "(.*)" (\d+) (\d+)/) {
            $log_messages_reached = 1;
            print "\n";

        } else {
            # Wrapped message
            $_ = "\n". $_  if /^(?:\d+\.\d+\.\d+\.\d+)/;
            $_ = " " . $_;
        }
        s@<BR>$@@;
        print;
        print "\n" unless $log_messages_reached;
    }
    print "\n";
}

sub VersionMessage {
    my $version_message;

    $version_message .= 'Privoxy-Log-Parser ' . PRIVOXY_LOG_PARSER_VERSION  . "\n";
    $version_message .= 'Copyright (C) 2007-2010 Fabian Keil <fk@fabiankeil.de>' . "\n";
    $version_message .= 'http://www.fabiankeil.de/sourcecode/privoxy-log-parser/' . "\n";

    print $version_message;
}

sub get_cli_options () {

    our %cli_options = (
        'html-output'              => CLI_OPTION_DEFAULT_TO_HTML_OUTPUT,
        'title'                    => CLI_OPTION_TITLE,
        'no-syntax-highlighting'   => CLI_OPTION_NO_SYNTAX_HIGHLIGHTING,
        'no-embedded-css'          => CLI_OPTION_NO_EMBEDDED_CSS,
        'no-msecs'                 => CLI_OPTION_NO_MSECS,
        'shorten-thread-ids'       => CLI_OPTION_SHORTEN_THREAD_IDS,
        'show-ineffective-filters' => CLI_OPTION_SHOW_INEFFECTIVE_FILTERS,
        'accept-unknown-messages'  => CLI_OPTION_ACCEPT_UNKNOWN_MESSAGES,
        'statistics'               => CLI_OPTION_STATISTICS,
        'url-statistics-threshold' => CLI_OPTION_URL_STATISTICS_THRESHOLD,
        'unbreak-lines-only'       => CLI_OPTION_UNBREAK_LINES_ONLY,
        'host-statistics-threshold'=> CLI_OPTION_HOST_STATISTICS_THRESHOLD,
    );

    GetOptions (
        'html-output'              => \$cli_options{'html-output'},
        'title'                    => \$cli_options{'title'},
        'no-syntax-highlighting'   => \$cli_options{'no-syntax-highlighting'},
        'no-embedded-css'          => \$cli_options{'no-embedded-css'},
        'no-msecs'                 => \$cli_options{'no-msecs'},
        'shorten-thread-ids'       => \$cli_options{'shorten-thread-ids'},
        'show-ineffective-filters' => \$cli_options{'show-ineffective-filters'},
        'accept-unknown-messages'  => \$cli_options{'accept-unknown-messages'},
        'statistics'               => \$cli_options{'statistics'},
        'unbreak-lines-only'       => \$cli_options{'unbreak-lines-only'},
        'url-statistics-threshold=i'=> \$cli_options{'url-statistics-threshold'},
        'host-statistics-threshold=i'=> \$cli_options{'host-statistics-threshold'},
        'version'                  => sub { VersionMessage && exit(0) },
        'help'                     => \&help,
   ) or exit(1);

   $html_output_mode = cli_option_is_set('html-output');
   $no_msecs_mode = cli_option_is_set('no-msecs');
   $shorten_thread_ids = cli_option_is_set('shorten-thread-ids');
   $line_end = get_line_end();
}

sub help () {

    our %cli_options;

    VersionMessage();

    print << "    EOF"

Options and their default values if they have any:
    [--accept-unknown-messages]
    [--host-statistics-threshold $cli_options{'host-statistics-threshold'}]
    [--html-output]
    [--no-embedded-css]
    [--no-msecs]
    [--no-syntax-highlighting]
    [--shorten-thread-ids]
    [--show-ineffective-filters]
    [--statistics]
    [--unbreak-lines-only]
    [--url-statistics-threshold $cli_options{'url-statistics-threshold'}]
    [--title $cli_options{'title'}]
    [--version]
see "perldoc $0" for more information
    EOF
    ;
    exit(0);
}

################################################################################
# main
################################################################################
sub main () {

    get_cli_options();
    set_background(DEFAULT_BACKGROUND);
    prepare_our_stuff();

    print_intro();

    # XXX: should explicitly reject incompatible argument combinations
    if (cli_option_is_set('unbreak-lines-only')) {
        unbreak_lines_only_loop();
    } elsif (cli_option_is_set('statistics')) {
        stats_loop();
    } else {
        parse_loop();
    }

    print_outro();
}

main();

=head1 NAME

B<privoxy-log-parser> - A parser and syntax-highlighter for Privoxy log messages

=head1 SYNOPSIS

B<privoxy-log-parser> [B<--accept-unknown-messages>] [B<--html-output>]
[B<--no-msecs>] [B<--no-syntax-higlighting>] [B<--statistics>]
[B<--shorten-thread-ids>] [B<--show-ineffective-filters>]
[B<--url-statistics-threshold>] [B<--version>]

=head1 DESCRIPTION

B<privoxy-log-parser> reads Privoxy log messages and

- syntax-highlights recognized lines,

- reformats some of them for easier comprehension,

- filters out less useful messages, and

- (in some cases) calculates additional information,
  like the compression ratio or how a filter affected
  the content size.

With B<privoxy-log-parser> you should be able to increase Privoxy's log level
without getting confused by the resulting amount of output. For example for
"debug 64" B<privoxy-log-parser> will (by default) only show messages that
affect the content. If a filter doesn't cause any hits, B<privoxy-log-parser>
will hide the "filter foo caused 0 hits" message.

=head1 OPTIONS

[B<--accept-unknown-messages>] Don't print warnings in case of unknown messages,
just don't highlight them.

[B<--host-statistics-threshold>] Only show the request count for a host
if it's above or equal to the given threshold. If the threshold is 0, host
statistics are disabled.

[B<--html-output>] Use HTML and CSS for the syntax highlighting. If this option is
omitted, ANSI escape sequences are used unless B<--no-syntax-highlighting> is active.
This option is only intended to make embedding log excerpts in web pages easier.
It does not escape any input!

[B<--no-msecs>] Don't expect milisecond resolution

[B<--no-syntax-highlighting>] Disable syntax-highlighting. Useful when
the filtered output is piped into less in which case the ANSI control
codes don't work, or if the terminal itself doesn't support the control
codes.

[B<--shorten-thread-ids>] Shorten the thread ids to a three-digit decimal number.
Note that the mapping from thread ids to shortened ids is created at run-time
and thus varies with the input.

[B<--show-ineffective-filters>] Don't suppress log lines for filters
that didn't modify the content.

[B<--statistics>] Gather various statistics instead of syntax highlighting
log messages. This is an experimental feature, if the results look wrong
they very well might be. Also note that the results are pretty much guaranteed
to be incorrect if Privoxy and Privoxy-Log-Parser aren't in sync.

[B<--unbreak-lines-only>] Tries to fix lines that got messed up by a broken or
interestingly configured mail client and thus are no longer recognized properly.
Only fixes some breakage, but may be good enough or at least better than nothing.
Doesn't do anything else, so you probably want to pipe the output into
B<privoxy-log-parser> again.

[B<--url-statistics-threshold>] Only show the request count for a ressource
if it's above or equal to the given threshold. If the threshold is 0, URL
statistics are disabled.

[B<--version>] Print version and exit.

=head1 EXAMPLES

To monitor a log file:

tail -F /usr/jails/privoxy-jail/var/log/privoxy/privoxy.log | B<privoxy-log-parser>

Replace '-F' with '-f' if your tail implementation lacks '-F' support
or if the log won't get rotated anyway. The log file location depends
on your system (Doh!).

To monitor Privoxy without having it write to a log file:

privoxy --no-daemon /usr/jails/privoxy-jail/usr/local/etc/privoxy/config 2>&1 | B<privoxy-log-parser>

Again, the config file location depends on your system. Output redirection
depends on your shell, the above works with bourne shells.

To read a processed Privoxy log file from top to bottom, letting the content
scroll by slightly faster than you can read:

B<privoxy-log-parser> < /usr/jails/privoxy-jail/var/log/privoxy/privoxy.log

This is probably only useful to fill screens in the background of haxor movies.

=head1 CAVEATS

Syntax highlighting with ANSI escape sequences will look strange
if your background color isn't black.

Some messages aren't recognized yet and will not be fully highlighted.

B<privoxy-log-parser> is developed with Privoxy 3.0.7 or later in mind,
using earlier Privoxy versions will probably result in an increased amount
of unrecognized log lines.

Privoxy's log files tend to be rather large. If you use HTML
highlighting some browsers can't handle them, get confused and
will eventually crash because of segmentation faults or unexpected
exceptions. This is a problem in the browser and not B<privoxy-log-parser>'s
fault.

=head1 BUGS

Many settings can't be controlled through command line options yet.

=head1 SEE ALSO

privoxy(1)

=head1 AUTHOR

Fabian Keil <fk@fabiankeil.de>

=cut
