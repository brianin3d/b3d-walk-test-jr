# WTF is this Sh!t!?

Nice language... what is wrong with you? Is that how you start of documentation? Really?!

Whatever... moving on.

I recently got a copy of the ferkin amazing book [The Animator's Survival Guide](http://www.amazon.com/The-Animators-Survival-Richard-Williams/dp/0571202284 ) and immediately became interested in trying some walk cycles [again](http://brianin3d-demos.appspot.com/static/demos/random/animation/walking/first.steps.in.walking.xml)

I'm always torn about how to best fnck this stuff up:

* pencil and paper
* inkscape
* blender

All good obvious choices on how to produce a real train wreck.

This time I went with inkscape again.

# Keep on walkin'!

The svg directory has some random experiments in it. Don't believe me? Look for yourself!

I put a simple (aka lame) background on the "bg" layer and each frame on a layer with a name like "frame1".

Sometimes I named them like "frame_9_1" to indicate frame 9 was just a reverse of "frame1".

I was hiding and exporting manually, which was terrible. Took forever and was a major bump between making changes and seeing them.

I knew inkscape had a command line export then I found [Daniel Albuschat's post](http://daniel-albuschat.blogspot.com/2013/03/export-layers-from-svg-files-to-png.html) about filtering out layers with xsl.

# Scripting it

Usage: frame_job.sh inkscape.svg

This script assumes the layers with names like "back..." and "bg.." are 
background layers which should be rendered on every frame.

The naming conventions for the frames should be either "frame..." or "layer..."

The naming is not case sensitive.

The first group of numbers in the name are taken to be the frame number. Frame #1
could be either "frame1" or "frame_1" or "frame_1_so_other_notes_or_#3"

The script does a bad job of trying to guess a delay. You can override it by 
export DELAY variable in the shell you run it from.

If you export KEEP_TMP_FILES, it will do just that as well.

Oh... you need xsltproc and inkscape installed and on the path. Also imagemagick for 
the animated gif.

If everything works right, you will have a file called "test.gif" and "test-{timestamped}.gif"
in pwd.

Good luck

# links

* http://www.amazon.com/Cartoon-Animation-Collectors-Preston-Blair/dp/1560100842/
* https://www.youtube.com/watch?v=_Wnn-LokCOc
* https://www.youtube.com/watch?v=kdfwmDnCsJg
