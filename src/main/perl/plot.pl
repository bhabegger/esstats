#!/usr/bin/perl

my %curEntry = ();
while(<>) {
  if(my ($anonid,$score) = m/\*\*\*\s*([0-9]+)\s*\*\*\*.* JSD:\s*([0-9\.]+)/) {
    print join(",", map { $curEntry{$_} } ("anonid","selfScore","matched","source1","score1","source2","score2","source3","score3")),"\n"; 
    %curEntry = ( anonid => $anonid, selfScore => $score , matched => 0);
  } elsif(my ($rank,$matched,$source,$score) = m/\[([1-3])\](\*?)\s+target:[0-9]+\s+->\s+source:([0-9]+)\s+\(([0-9\.]+)\)/) {
    $curEntry{"source" . $rank} = $source;
    $curEntry{"score" . $rank} = $score;
    if($matched eq '*') {
      $curEntry{"matched"} = $rank;
    }
  }
}
print join(",", map { $curEntry{$_} } ("anonid","selfScore","matched","source1","score1","source2","score2","source3","score3")),"\n"; 
