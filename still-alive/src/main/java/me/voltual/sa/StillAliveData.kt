package me.voltual.sa

public data class LyricItem(
	val words: Any, // String or Int (ASCII art index)
	val timeMs: Long,
	val intervalSec: Double,
	val mode: Int,
)

public object StillAliveData {

	public val asciiArt: List<List<String>> = listOf(
		// a1
		listOf(
			"              .,-:;//;:=,               ",
			"          . :H@@@MM@M#H/.,+%;,          ",
			"       ,/X+ +M@@M@MM%=,-%HMMM@X/,       ",
			"     -+@MM; #M@@MH+-,;XMMMM@MMMM@+-     ",
			"    ;@M@@M- XM@X;. -+XXXXXHHH@M@M#@/.   ",
			"  ,%MM@@MH ,@%=            .---=-=:=,.  ",
			"  =@#@@@MX .,              -%HX##%%%+;  ",
			" =-./@M@M\$                  .;@MMMM@MM: ",
			" X@/ -#MM/                    .+MM@@@M\$ ",
			",@M@H: :@:                    . =X#@@@@-",
			",@@@MMX, .                    /H- ;@M@M=",
			".H@@@@M@+,                    %MM+..%#\$.",
			" /MMMM@MMH/.                  XM@MH; =; ",
			"  /%+%#XHH@\$=              , .H@@@@MX,  ",
			"   .=--------.           -%H.,@@@@@MX,  ",
			"   .%MM@@@HHHXX###%+= .:#MMX =M@@MM%.   ",
			"     =XMMM@MM@MM#H;,-+HMM@M+ /MMMX=     ",
			"       =%@M@M#@\$-.=#@MM@@@M; %M%=       ",
			"         ,:+\$+-,/H#MMMMMMM@= =,         ",
			"               =++%%%%+/:-.             ",
		),
		// a2
		listOf(
			"             =+\$HM####@H%;,             ",
			"          /H###############M\$,          ",
			"          ,@################+           ",
			"           .H##############+            ",
			"             X############/             ",
			"              \$##########/              ",
			"               %########/               ",
			"                /X/;;+X/                ",
			"                 -XHHX-                 ",
			"                ,######,                ",
			"#############X  .M####M.  X#############",
			"##############-   -//-   -##############",
			"X##############%,      ,+##############X",
			"-##############X        X##############-",
			" %############%          %############% ",
			"  %##########;            ;##########%  ",
			"   ;#######M=              =M#######;   ",
			"    .+M###@,                ,@###M+.    ",
			"       :XH.                  .HX:       ",
			"                                        ",
		),
		// a3
		listOf(
			"                 =/;;/-                 ",
			"                +:    //                ",
			"               /;      /;               ",
			"              -X        H.              ",
			".//;;;:;;-,   X=        :+   .-;:=;:;%;.",
			"M-       ,=;;;#:,      ,:#;;:=,       ,@",
			":%           :%.=/++++/=.\$=           %=",
			" ,%;         %/:+/;,,/++:+/         ;+. ",
			"   ,+/.    ,;@+,        ,%H;,    ,/+,   ",
			"      ;+;;/= @.  .H##X   -X :///+;      ",
			"      ;+=;;;.@,  .XM@\$.  =X.//;=%/.     ",
			"   ,;:      :@%=        =\$H:     .+%-   ",
			" ,%=         %;-///==///-//         =%, ",
			";+           :%-;;;:;;;;-X-           +:",
			"@-      .-;;;;M-        =M/;;;-.      -X",
			" :;;::;;-.    %-        :+    ,-;;-;:== ",
			"              ,X        H.              ",
			"               ;/      %=               ",
			"                //    +;                ",
			"                 ,////,                 ",
		),
		// a4
		listOf(
			"                          .,---.        ",
			"                        ,/XM#MMMX;,     ",
			"                      -%##########M%,   ",
			"                     -@######%  \$###@=  ",
			"      .,--,         -H#######\$   \$###M: ",
			"   ,;\$M###MMX;     .;##########\$;HM###X=",
			" ,/@##########H=      ;################+",
			"-+#############M/,      %##############+",
			"%M###############=      /##############:",
			"H################      .M#############;.",
			"@###############M      ,@###########M:. ",
			"X################,      -\$=X#######@:   ",
			"/@##################%-     +######\$-    ",
			".;##################X     .X#####+,     ",
			" .;H################/     -X####+.      ",
			"   ,;X##############,       .MM/        ",
			"      ,:+\$H@M#######M#\$-    .\$\$=        ",
			"           .,-=;+\$@###X:    ;/=.        ",
			"                  .,/X\$;   .::,         ",
			"                      .,    ..          ",
		),
		// a5
		listOf(
			"            .+                          ",
			"             /M;                        ",
			"              H#@:              ;,      ",
			"              -###H-          -@/       ",
			"               %####\$.  -;  .%#X        ",
			"                M#####+;#H :M#M.        ",
			"..          .+/;%#########X###-         ",
			" -/%H%+;-,    +##############/          ",
			"    .:\$M###MH\$%+############X  ,--=;-   ",
			"        -/H#####################H+=.    ",
			"           .+#################X.        ",
			"         =%M####################H;.     ",
			"            /@###############+;;/%%;,   ",
			"         -%###################\$.        ",
			"       ;H######################M=       ",
			"    ,%#####MH\$%;+#####M###-/@####%      ",
			"  :\$H%+;=-      -####X.,H#   -+M##@-    ",
			" .              ,###;    ;      =\$##+   ",
			"                .#H,               :XH, ",
			"                 +                   .;-",
		),
		// a6
		listOf(
			"                     -\$-                ",
			"                    .H##H,              ",
			"                   +######+             ",
			"                .+#########H.           ",
			"              -\$############@.          ",
			"            =H###############@  -X:     ",
			"          .\$##################:  @#@-   ",
			"     ,;  .M###################;  H###;  ",
			"   ;@#:  @###################@  ,#####: ",
			" -M###.  M#################@.  ;######H ",
			" M####-  +###############\$   =@#######X ",
			" H####\$   -M###########+   :#########M, ",
			"  /####X-   =########%   :M########@/.  ",
			"    ,;%H@X;   .\$###X   :##MM@%+;:-      ",
			"                 ..                     ",
			"  -/;:-,.              ,,-==+M########H ",
			" -##################@HX%%+%%\$%%%+:,,    ",
			"   .-/H%%%+%%\$H@###############M@+=:/+: ",
			"/XHX%:#####MH%=    ,---:;;;;/%%XHM,:###\$",
			"\$@#MX %+;-                           .  ",
		),
		// a7
		listOf(
			"                                     :X-",
			"                                  :X### ",
			"                                ;@####@ ",
			"                              ;M######X ",
			"                            -@########\$ ",
			"                          .\$##########@ ",
			"                         =M############-",
			"                        +##############\$",
			"                      .H############\$=. ",
			"         ,/:         ,M##########M;.    ",
			"      -+@###;       =##########M;       ",
			"   =%M#######;     :#########M/         ",
			"-\$M###########;   :#########/           ",
			" ,;X###########; =########\$.            ",
			"     ;H#########+#######M=              ",
			"       ,+##############+                ",
			"          /M#########@-                 ",
			"            ;M######%                   ",
			"              +####:                    ",
			"               ,\$M-                     ",
		),
		// a8
		listOf(
			"           .-;+\$XHHHHHHX\$+;-.           ",
			"        ,;X@@X%/;=----=:/%X@@X/,        ",
			"      =\$@@%=.              .=+H@X:      ",
			"    -XMX:                      =XMX=    ",
			"   /@@:                          =H@+   ",
			"  %@X,                            .\$@\$  ",
			" +@X.                               \$@% ",
			"-@@,                                .@@=",
			"%@%                                  +@\$",
			"H@:                                  :@H",
			"H@:         :HHHHHHHHHHHHHHHHHHX,    =@H",
			"%@%         ;@M@@@@@@@@@@@@@@@@@H-   +@\$",
			"=@@,        :@@@@@@@@@@@@@@@@@@@@@= .@@:",
			" +@X        :@@@@@@@@@@@@@@@M@@@@@@:%@% ",
			"  \$@\$,      ;@@@@@@@@@@@@@@@@@M@@@@@@\$. ",
			"   +@@HHHHHHH@@@@@@@@@@@@@@@@@@@@@@@+   ",
			"    =X@@@@@@@@@@@@@@@@@@@@@@@@@@@@X=    ",
			"      :\$@@@@@@@@@@@@@@@@@@@M@@@@\$:      ",
			"        ,;\$@@@@@@@@@@@@@@@@@@X/-        ",
			"           .-;+\$XXHHHHHX\$+;-.           ",
		),
		// a9
		listOf(
			"            ,:/+/-                      ",
			"            /M/              .,-=;//;-  ",
			"       .:/= ;MH/,    ,=/+%\$XH@MM#@:     ",
			"      -\$##@+\$###@H@MMM#######H:.    -/H#",
			" .,H@H@ X######@ -H#####@+-     -+H###@ ",
			"  .,@##H;      +XM##M/,     =%@###@X;-  ",
			"X%-  :M##########\$.    .:%M###@%:       ",
			"M##H,   +H@@@\$/-.  ,;\$M###@%,          -",
			"M####M=,,---,.-%%H####M\$:          ,+@##",
			"@##################@/.         :%H##@\$- ",
			"M###############H,         ;HM##M\$=     ",
			"#################.    .=\$M##M\$=         ",
			"################H..;XM##M\$=          .:+",
			"M###################@%=           =+@MH%",
			"@################M/.          =+H#X%=   ",
			"=+M##############M,       -/X#X+;.      ",
			"  .;XM##########H=    ,/X#H+:,          ",
			"     .=+HM######M+/+HM@+=.              ",
			"         ,:/%XM####H/.                  ",
			"              ,.:=-.                    ",
		),
		// a10
		listOf(
			"       #+ @      # #              M#@   ",
			" .    .X  X.%##@;# #   +@#######X. @#%  ",
			"   ,==.   ,######M+  -#####%M####M-    #",
			"  :H##M%:=##+ .M##M,;#####/+#######% ,M#",
			" .M########=  =@#@.=#####M=M#######=  X#",
			" :@@MMM##M.  -##M.,#######M#######. =  M",
			"             @##..###:.    .H####. @@ X,",
			"   ############: ###,/####;  /##= @#. M ",
			"           ,M## ;##,@#M;/M#M  @# X#% X# ",
			".%=   ######M## ##.M#:   ./#M ,M #M ,#\$ ",
			"##/         \$## #+;#: #### ;#/ M M- @# :",
			"#+ #M@MM###M-;M #:\$#-##\$H# .#X @ + \$#. #",
			"      ######/.: #%=# M#:MM./#.-#  @#: H#",
			"+,.=   @###: /@ %#,@  ##@X #,-#@.##% .@#",
			"#####+;/##/ @##  @#,+       /#M    . X, ",
			"   ;###M#@ M###H .#M-     ,##M  ;@@; ###",
			"   .M#M##H ;####X ,@#######M/ -M###\$  -H",
			"    .M###%  X####H  .@@MM@;  ;@#M@      ",
			"      H#M    /@####/      ,++.  / ==-,  ",
			"               ,=/:, .+X@MMH@#H  #####\$=\"",
		),
	)

	public val rawLyrics: List<LyricItem> = listOf(
		// Page 1
		LyricItem("Forms FORM-29827281-12:", 0, -1.0, 0),
		LyricItem("Test Assessment Report", 2000, -1.0, 0),
		LyricItem("\u0000\u0000\u0000\u0000\u0000\u0000\u0000", 4000, -1.0, 0),
		LyricItem("", 7100, 0.0, 4), // Music start
		LyricItem("This was a triumph.", 7300, 2.0, 0),
		LyricItem("", 9300, 0.0, 5), // Credits start
		LyricItem("I'm making a note here:", 11230, 2.0, 0),
		LyricItem("HUGE SUCCESS.", 13470, 1.7, 0),
		LyricItem("It's hard to overstate", 16270, -1.0, 0),
		LyricItem("my satisfaction.", 18730, 2.6, 0),
		LyricItem("Aperture Science", 23500, 1.8, 0),
		LyricItem(0, 23500, 0.0, 2), // ASCII 1
		LyricItem("We do what we must", 27330, 1.6, 0),
		LyricItem("because we can.", 29100, 1.5, 0),
		LyricItem("For the good of all of us.", 32370, -1.0, 0),
		LyricItem(1, 35000, 0.0, 2), // ASCII 2
		LyricItem("Except the ones who are dead.", 35670, -1.0, 0),
		LyricItem("", 37170, 0.05, 0),
		LyricItem(0, 37170, 0.0, 2), // ASCII 1
		LyricItem("But there's no sense crying", 37870, -1.0, 0),
		LyricItem("over every mistake.", 39730, 1.77, 0),
		LyricItem("You just keep on trying", 41700, -1.0, 0),
		LyricItem("till you run out of cake.", 43700, -1.0, 0),
		LyricItem(2, 45000, 0.0, 2), // ASCII 3
		LyricItem("And the Science gets done.", 45700, -1.0, 0),
		LyricItem("And you make a neat gun.", 47670, -1.0, 0),
		LyricItem(0, 49030, 0.0, 2), // ASCII 1
		LyricItem("For the people who are", 49730, -1.0, 0),
		LyricItem("still alive.", 51100, 1.6, 1),

		// Page 2
		LyricItem(0, 53530, 0.0, 3), // Clear lyrics
		LyricItem("Forms FORM-55551-5:", 54130, -1.0, 0),
		LyricItem("Personnel File Addendum:", 54770, 1.13, 0),
		LyricItem("", 56500, 0.05, 0),
		LyricItem("Dear <<Subject Name Here>>,", 56500, -1.0, 0),
		LyricItem("", 59000, -1.0, 0),
		LyricItem("I'm not even angry.", 59000, 1.86, 0),
		LyricItem("I'm being ", 63200, -1.0, 1),
		LyricItem("so ", 64130, -1.0, 1),
		LyricItem("sincere right now.", 64700, 1.9, 0),
		LyricItem("Even though you broke ", 68270, -1.0, 1),
		LyricItem(3, 70200, 0.0, 2), // ASCII 4
		LyricItem("my heart.", 70900, -1.0, 0),
		LyricItem("And killed me.", 71700, 1.43, 0),
		LyricItem(4, 73000, 0.0, 2), // ASCII 5
		LyricItem("And tore me to pieces.", 75000, 1.83, 0),
		LyricItem("And threw every piece ", 79000, -1.0, 1),
		LyricItem("into a fire.", 80800, 1.8, 0),
		LyricItem(5, 80800, 0.0, 2), // ASCII 6
		LyricItem("As they burned it hurt because", 84300, -1.0, 0),
		LyricItem(6, 86900, 0.0, 2), // ASCII 7
		LyricItem("I was so happy for you!", 87600, 1.67, 0),
		LyricItem("Now, these points of data", 89600, -1.0, 0),
		LyricItem("make a beautiful line.", 91670, -1.0, 0),
		LyricItem("And we're out of beta.", 93570, -1.0, 0),
		LyricItem("We're releasing on time.", 95600, -1.0, 0),
		LyricItem(4, 97000, 0.0, 2), // ASCII 5
		LyricItem("So I'm GLaD I got burned.", 97700, -1.0, 0),
		LyricItem(2, 99130, 0.0, 2), // ASCII 3
		LyricItem("Think of all the things we learned", 99830, -1.0, 0),
		LyricItem(0, 101200, 0.0, 2), // ASCII 1
		LyricItem("For the people who are", 101900, -1.0, 0),
		LyricItem("Still alive.", 103270, 1.8, 0),

		// Page 3
		LyricItem(0, 106030, 0.0, 3), // Clear lyrics
		LyricItem("Forms FORM-55551-6:", 106630, -1.0, 0),
		LyricItem("Personnel File Addendum Addendum:", 107100, 1.36, 0),
		LyricItem("", 107100, 0.05, 0),
		LyricItem("One last thing:", 109100, -1.0, 0),
		LyricItem("", 111300, 0.05, 0),
		LyricItem("Go ahead and leave ", 111300, -1.0, 1),
		LyricItem("me.", 112800, 0.5, 0),
		LyricItem("I think I'd prefer to stay ", 115070, -1.0, 1),
		LyricItem("inside.", 117870, 1.13, 0),
		LyricItem("Maybe you'll find someone else", 120370, -1.0, 0),
		LyricItem("To help you.", 123900, 1.23, 0),
		LyricItem("Maybe Black ", 127370, -1.0, 1),
		LyricItem(7, 127870, 0.0, 2), // ASCII 8
		LyricItem("Mesa...", 128570, 2.7, 0),
		LyricItem("THAT WAS A JOKE.", 131370, 1.46, 1),
		LyricItem(" FAT CHANCE.", 133870, 1.1, 0),
		LyricItem("Anyway, ", 136200, -1.0, 1),
		LyricItem(8, 136700, 0.0, 2), // ASCII 9
		LyricItem("this cake is great.", 137400, -1.0, 0),
		LyricItem("It's so delicious and moist.", 139630, -1.0, 0),
		LyricItem(9, 141230, 0.0, 2), // ASCII 10
		LyricItem("Look at me still talking", 141930, -1.0, 0),
		LyricItem(1, 143200, 0.0, 2), // ASCII 2
		LyricItem("when there's science to do.", 143900, -1.0, 0),
		LyricItem(0, 145270, 0.0, 2), // ASCII 1
		LyricItem("When I look out there,", 145970, -1.0, 0),
		LyricItem("It makes me GLaD I'm not you.", 147670, -1.0, 0),
		LyricItem(2, 149130, 0.0, 2), // ASCII 3
		LyricItem("I've experiments to run.", 149830, -1.0, 0),
		LyricItem(4, 151200, 0.0, 2), // ASCII 5
		LyricItem("There is research to be done.", 151900, -1.0, 0),
		LyricItem(0, 153200, 0.0, 2), // ASCII 1
		LyricItem("On the people who are", 153900, -1.0, 0),
		LyricItem("still alive", 155530, 2.0, 1),

		// Page 4
		LyricItem(0, 156970, 0.0, 3), // Clear lyrics
		LyricItem("", 157570, 0.05, 0),
		LyricItem("", 157570, 0.05, 0),
		LyricItem("", 157570, 0.05, 0),
		LyricItem("PS: And believe me I am", 157570, -1.0, 0),
		LyricItem("still alive.", 159600, 1.13, 0),
		LyricItem("PPS: I'm doing Science and I'm", 161500, -1.0, 0),
		LyricItem("still alive.", 163630, 1.13, 0),
		LyricItem("PPPS: I feel FANTASTIC and I'm", 165500, -1.0, 0),
		LyricItem("still alive.", 167600, -1.0, 0),
		LyricItem("", 168600, -1.0, 0),
		LyricItem("FINAL THOUGH:", 168600, -1.0, 0),
		LyricItem("While you're dying I'll be", 169930, -1.0, 0),
		LyricItem("still alive.", 171570, -1.0, 0),
		LyricItem("", 172770, -1.0, 0),
		LyricItem("FINAL THOUGH PS:", 172770, -1.0, 0),
		LyricItem("And when you're dead I will be", 173670, -1.0, 0),
		LyricItem("still alive.", 175500, 1.13, 0),
		LyricItem("", 175500, -1.0, 0),
		LyricItem("", 175500, 0.05, 0),
		LyricItem("STILL ALIVE", 177600, 1.13, 0),
		LyricItem(0, 179000, 0.0, 3),
		LyricItem(0, 185000, 0.0, 3),
		LyricItem("ENDENDENDENDENDENDENDEND", 185000, 0.05, 9),
	)

	public const val CREDITS_TEXT: String = """>LIST PERSONNEL
            
Gautam Babbar
Ted Backman
Kelly Bailey
Jeff Ballinger
Aaron Barber
Jeep Barnett
Jeremy Bennett
Dan Berger
Yahn Bernier
Ken Birdwell
Derrick BirumMike Blaszczak
Iestyn Bleasdale-Shepherd
Chris Bokitch
Steve Bond
Matt Boone
Antoine Bourdon
Jamaal Bradley
Jason Brashill
Charlie Brown
Charlie Burgin
Andrew Burke
Augusta Butlin
Julie Caldwell
Dario Casali
Chris Chin
Jess Cliffe
Phil Co
John Cook
Christen Coomer
Greg Coomer
Scott Dalton
Kerry Davis
Jason Deakins
Joe Demers
Ariel Diaz
Quintin Doroquez
Jim Dose
Chris Douglass
Laura Dubuk
Mike Dunkle
Mike Durand
Mike Dussault
Dhabih Eng
Katie Engel
Chet Faliszek
Adrian Finol
Bill Fletcher
Moby Francke
Stephane Gaudette
Kathy Gehrig
Vitaliy Genkin
Paul Graham
Chris Green
Chris Grinstead
John Guthrie
Aaron Halifax
Reagan Halifax
Leslie Hall
Jeff Hameluck
Joe Han
Don Holden
Jason Holtman
Gray Horsfield
Keith Huggins
Jim Hughes
Jon Huisingh
Brian Jacobson
Lars Jensvold
Erik Johnson
Jakob Jungels
Rich Kaethler
Steve Kalning
Aaron Kearly
Iikka Keranen
David Kircher
Eric Kirchmer
Scott Klintworth
Alden Kroll
Marc Laidlaw
Jeff Lane
Tim Larkin
Dan LeFree
Isabelle LeMay
Tom Leonard
Jeff Lind
Doug Lombardi
Bianca Loomis
Richard Lord
Realm Lovejoy
Randy Lundeen
Scott Lynch
Ido Magal
Nick Maggiore
John McCaskey
Patrick McClard
Steve McClure
Hamish McKenzie
Gary McTaggart
Jason Mitchell
Mike Morasky
John Morello II
Bryn Moslow
Arsenio Navarro
Gabe Newell
Milton Ngan
Jake Nicholson
Martin Otten
Nick Papineau
Karen Prell
Bay Raitt
Tristan Reidford
Alfred Reynolds
Matt Rhoten
Garret Rickey
Dave Riller
Elan Ruskin
Matthew Russell
Jason Ruymen
David Sawyer
Marc Scaparro
Wade Schin
Matthew Scott
Aaron Seeler
Jennifer Seeley
Taylor Sherman
Eric Smith
Jeff Sorensen
David Speyrer
Jay Stelly
Jeremy Stone
Eric Strand
Kim Swift
Kelly Thornton
Eric Twelker
Carl Uhlman
Doug Valente
Bill Van Buren
Gabe Van Engel
Alex Vlachos
Robin Walker
Joshua Weier
Andrea Wicklund
Greg Winkler
Erik Wolpaw
Doug Wood
Matt T. Wood
Danika Wright
Matt Wright
Shawn Zabecki
Torsten Zabka 
            
            
'Still Alive' by:
Jonathan Coulton
            
Voices:
Ellen McLain - GlaDOS, Turrets
Mike Patton - THE ANGER SPHERE
            
Voice Casting:
Shana Landsburg\\Teri Fiddleman
            
Voice Recording:
Pure Audio, Seattle, WA
            
Voice recording
scheduling and logistics:
Pat Cockburn, Pure Audio
            
Translations:
SDL
            
Crack Legal Team:
Liam Lavery
Karl Quackenbush
Kristen Boraas
Kevin Rosenfield
Alan Bruggeman
Dennis Tessier
            
Thanks for the use of their face:
Alesia Glidewell - Chell
            
Special thanks to everyone at:
Alienware
ATI
Dell
Falcon Northwest
Havok
SOFTIMAGE
and Don Kemmis, SLK Technologies
            
            
THANK YOU FOR PARTICIPATING
IN THIS
ENRICHMENT CENTER ACTIVITY!!"""
}