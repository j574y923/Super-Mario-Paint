package smp.presenters.buttons.PlayPresenter;

/**
 * This class runs an arrangement instead of just a song.
 */
class ArrangementTask extends AnimationTask {

    @Override
    protected Staff call() throws Exception {
        highlightsOff();
        ArrayList<StaffSequence> seq = theArrangement.getTheSequences();
        ArrayList<File> files = theArrangement.getTheSequenceFiles();
        for (int i = 0; i < seq.size(); i++) {
            while (queue > 0)
                ;
            /* Force emptying of queue before changing songs. */
            queue++;
            setSoundset(seq.get(i).getSoundset());
            index = 0;
            advance = false;
            queue++;
            highlightSong(i);
            theSequence = seq.get(i);
            theSequenceFile = files.get(i);
            StateMachine.setNoteExtensions(
                    theSequence.getNoteExtensions());
            controller.getInstBLine().updateNoteExtensions();
            StateMachine.setTempo(theSequence.getTempo());
            queue++;
            updateCurrTempo();
            queue++;
            setScrollbar();
            lastLine = findLastLine();
            songPlaying = true;
            setTempo(theSequence.getTempo());
            playBars = staffImages.getPlayBars();
            int counter = 0;
            StateMachine.setMeasureLineNum(0);
            queue++;
            zeroEverything();
            while (queue > 0)
                ;
            /* Force operations to complete before starting a song. */
            while (songPlaying && arrPlaying) {
                queue++;
                playNextLine();
                counter++;
                if (counter > lastLine && counter % 4 == 0) {
                    songPlaying = false;
                }
                try {
                    Thread.sleep(delayMillis, delayNanos);
                } catch (InterruptedException e) {
                    // Do nothing
                }
            }
            if (!arrPlaying)
                break;
        }
        highlightsOff();
        hitStop();
        return theMatrix.getStaff();
    }
