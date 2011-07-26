class CreateParticipants < ActiveRecord::Migration
  def self.up
    create_table :participants do |t|
	  t.column :name, :string
	  t.column :telno, :string
    end
  end

  def self.down
    drop_table :participants
  end
end
