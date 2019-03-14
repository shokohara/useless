package com.github.shokohara.seed

import scalaz.Scalaz._

object Main {

  def main(args: Array[String]): Unit =
    """/usr/bin/ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"""".node(
      """
        |System Preferences > General > Appearance > [x] Use dark menu bar and Dock
        |System Preferences > General > Appearance > [x] Automatically hide and show the menu bar
        |System Preferences > Dock > [x] Automatically hide and show the Dock
        |System Preferences > Dock > Size: Small
        |System Preferences > Mission Control > [ ] Automatically rearrange Spaces based on most recent use
        |System Preferences > Mission Control > [x] Group windows by application
        |System Preferences > Mission Control > Keyboard and Mouse Shortcuts > Disable all
        |System Preferences > Mission Control > Language & Region > Add 日本語
        |System Preferences > Security & Privacy > [x] Require password immediately after sleep or screen saver begins
        |System Preferences > Keyboard > Keyboard > Key Repeat
        |System Preferences > Keyboard > Keyboard > Delay Until Repeat
        |System Preferences > Keyboard > Keyboard > [x] User all F1, F2, etc, keys as standard function keys
        |System Preferences > Keyboard > Shortcuts > Mission Control > [ ] Mission Control
        |System Preferences > Keyboard > Shortcuts > Keyboard > [ ] Disable all check boxes
        |System Preferences > Keyboard > Shortcuts > Input Source > [x] Select the previous input source
        |System Preferences > Keyboard > Shortcuts > Input Source > [x] Select the previous input source
        |System Preferences > Keyboard > Shortcuts > Spotlight > [x] Show Spotlight Search
        |System Preferences > Keyboard > Shortcuts > Spotlight > [ ] Show Finder search window
        |System Preferences > Keyboard > Input Sources > Add Hiragana (Google)
        |System Preferences > Keyboard > Modifier Keys... > Caps Lock Key: Control
        |System Preferences > Keyboard > Accecibility > Uncheck all boxex
        |System Preferences > Keyboard > App Shortcuts > Uncheck all boxex
        |System Preferences > Trackpad > [x] Tap to click
        |System Preferences > Trackpad > Point & Click > Tracking speed: Fast
        |System Preferences > Trackpad > More Gestures > [x] App Exposé
        |System Preferences > Sharing > Change Computer Name
        |System Preferences > Sharing > [x] Screen Sharing
        |System Preferences > Sharing > [x] Remote Login
        |System Preferences > iCloud > [x] Back to My Mac
      """.stripMargin.leaf,
      "sudo visudo\nYOUR_USER_NAME ALL=(ALL) NOPASSWD: ALL".node(
        "brew cask install intellij-toolbox".node(
          "Manual: Install intellij".node(
            "Manual: Setting repository".leaf
          )
        ),
        "brew cask install google-japanese-input ".node(
          "Start google japanese ime and RESTART".node(
            "Manual: Google Japanese Input Preference".leaf,
            "iterm2".node(
              """
                |General > Closing > UNCHECK Confirm closing multiple sessions
                |General > Closing > UNCHECK Confirm "Quit iTerm2" command
                |General > Closing > CHECK Smart window placement
                |General > Closing > UNCHECK Adjust window when changing font size
                |General > Closing > UNCHECK Native full screen windows
                |Appearance > Tabs > Theme > Dark
                |Appearance > Tabs > Theme > UNCHECK ALL
                |Appearance > Tabs > Panes > UNCHECK Show per-pane title bar with split panes
                |Appearance > UNCHECK Show current job name
                |Appearance > Window > CHECK Hide scrollbars
                |Keys > HotKey > CHECK Show/hide iTerm2 with a system-wide hotkey
                |Keys > HotKey > CHECK Hotkey toggles a dedicated window with profile
                |Keys > HotKey > UNCHECK Hotkey window hides when focus is lost
                |Profiles > SELECT Hotkey Window > Window > Style > CHECK Show/hide iTerm2 with a system-wide hotkey
                |Profiles > SELECT Hotkey Window > Terminal > Scrollback Lines > INPUT 0
                |Profiles > SELECT Hotkey Window > Keys > Left option key acts as > INPUT +Esc
                |Profiles > SELECT Hotkey Window > Keys > Right option key acts as > INPUT +Esc
                |Profiles > Hotkey window set as default
              """.stripMargin.node(
                "dotfiles".node(
                  "tmux".node(
                    """ssh-keygen -t ed25519 -f $HOME/.ssh/id_ed25519 -q -N """"".node(
                      "ghq home".node(
                        "ghq ???".leaf
                      ),
                      "go home".node(
                        "about go".leaf
                      )
                    )
                  )
                )
              )
            )
          )
        ),
        "brew install java ???".node(
          "brew install sbt".node(
            "全部 sbt updateしてキャッシュを作る".leaf
          )
        )
      )
    )
}
