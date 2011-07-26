class AddTestData < ActiveRecord::Migration
  def self.up
    Conference.delete_all
    Conference.create(:name => 'Test conference 1', :time => '14:15')
    Conference.create(:name => 'Test conference 2', :time => '10:20')

    Participant.delete_all
    Participant.create(:name => 'Adrian Smith', :telno => '01442208294')
    Participant.create(:name => 'Raghav Ramesh', :telno => '07918039798')
    Participant.create(:name => 'Robbie Clutton', :telno => '07918196706')
    
    ConferencesParticipants.delete_all
    Conference.find(:first, :conditions => ["name = ?", 'Test conference 1']).participants << Participant.find(:first, :conditions => ["name = ?", 'Adrian Smith'])
    Conference.find(:first, :conditions => ["name = ?", 'Test conference 1']).participants << Participant.find(:first, :conditions => ["name = ?", 'Raghav Ramesh'])
    Conference.find(:first, :conditions => ["name = ?", 'Test conference 2']).participants << Participant.find(:first, :conditions => ["name = ?", 'Robbie Clutton'])
  end

  def self.down
    Conference.delete_all
    Participant.delete_all
    ConferencesParticipants.delete_all
  end
end
