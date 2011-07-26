class CreateConferencesParticipants < ActiveRecord::Migration
  def self.up
    create_table :conferences_participants, :id => false do |t|
	  t.column :conference_id, :integer
	  t.column :participant_id, :integer
    end
  end

  def self.down
    drop_table :conferences_participants
  end
end
